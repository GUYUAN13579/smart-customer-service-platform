package com.example.smartcustomerservice.service.impl.file;

import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.config.properties.MinioProperties;
import com.example.smartcustomerservice.domain.entity.FileResource;
import com.example.smartcustomerservice.domain.vo.FileContentVO;
import com.example.smartcustomerservice.domain.vo.FileResourceVO;
import com.example.smartcustomerservice.mapper.file.FileResourceMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.file.FileResourceService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileResourceServiceImpl implements FileResourceService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final FileResourceMapper fileResourceMapper;

    public FileResourceServiceImpl(MinioClient minioClient,
                                   MinioProperties minioProperties,
                                   FileResourceMapper fileResourceMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.fileResourceMapper = fileResourceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO uploadFile(MultipartFile file, Long uploaderId) {
        return upload(file, uploaderId, "files");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO uploadImage(MultipartFile file, Long uploaderId) {
        if(file == null || file.isEmpty())
            throw new BusinessException(ResultCode.FILE_NOT_EXIST, "文件不存在");
        if(file.getContentType() == null || !file.getContentType().startsWith("image/"))
            throw new BusinessException(ResultCode.NOT_IMAGES, "上传类型不是images");
        return upload(file, uploaderId, "images");
    }

    @Override
    public FileResourceVO getFile(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件ID不能为空");
        }
        FileResource fileResource = fileResourceMapper.selectById(id);
        if (fileResource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        return toVO(fileResource);
    }

    @Override
    public FileContentVO getFileContent(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件ID不能为空");
        }
        FileResource fileResource = fileResourceMapper.selectById(id);
        if (fileResource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }

        try (var inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(fileResource.getStoragePath())
                        .build())) {
            FileContentVO vo = new FileContentVO();
            vo.setOriginalName(fileResource.getOriginalName());
            vo.setContentType(fileResource.getContentType());
            vo.setContent(inputStream.readAllBytes());
            return vo;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "读取文件失败");
        }
    }

    private FileResourceVO upload(MultipartFile file, Long uploaderId, String directory) {
        if(file == null || file.isEmpty())
            throw new BusinessException(ResultCode.FILE_NOT_EXIST, "文件不存在");

        String datePath = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename();
        String safeOriginalName = originalName == null || originalName.isBlank()
                ? "unknown"
                : originalName.replaceAll("[\\\\/]", "_");
        String objectName = String.format("%s/%s/%s-%s", directory, datePath, uuid, safeOriginalName);

        try {
            boolean flag = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .build()
            );
            if(flag == false)
            {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "初始化文件存储失败");
        }

        try {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "上传文件失败");
        }

        FileResource fileResource = new FileResource();
        fileResource.setOriginalName(originalName);
        fileResource.setStoragePath(objectName);
        fileResource.setContentType(file.getContentType());
        fileResource.setFileSize(file.getSize());
        fileResource.setUploaderId(resolveUploaderId(uploaderId));
        fileResource.setCreatedAt(LocalDateTime.now());

        if (fileResourceMapper.insert(fileResource) == 0) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "保存文件记录失败");
        }

        return toVO(fileResource);
    }

    private Long resolveUploaderId(Long uploaderId) {
        return uploaderId != null ? uploaderId : SecurityUtils.getCurrentUserId();
    }

    private FileResourceVO toVO(FileResource fileResource) {
        String url = minioProperties.getPublicUrl()
                + "/" + minioProperties.getBucket()
                + "/" + fileResource.getStoragePath();

        FileResourceVO vo = new FileResourceVO();
        vo.setId(fileResource.getId());
        vo.setOriginalName(fileResource.getOriginalName());
        vo.setStoragePath(fileResource.getStoragePath());
        vo.setContentType(fileResource.getContentType());
        vo.setFileSize(fileResource.getFileSize());
        vo.setUploaderId(fileResource.getUploaderId());
        vo.setCreatedAt(fileResource.getCreatedAt());
        vo.setUrl(url);
        return vo;
    }
}
