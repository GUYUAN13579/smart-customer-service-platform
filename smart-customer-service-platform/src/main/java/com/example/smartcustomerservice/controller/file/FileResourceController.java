package com.example.smartcustomerservice.controller.file;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.vo.FileContentVO;
import com.example.smartcustomerservice.domain.vo.FileResourceVO;
import com.example.smartcustomerservice.service.file.FileResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件资源")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/files")
public class FileResourceController {

    private final FileResourceService fileResourceService;

    public FileResourceController(FileResourceService fileResourceService) {
        this.fileResourceService = fileResourceService;
    }

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:upload')")
    public ApiResult<FileResourceVO> uploadFile(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return ApiResult.success(fileResourceService.uploadFile(file, uploaderId));
    }

    @Operation(summary = "上传图片")
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file:image:upload')")
    public ApiResult<FileResourceVO> uploadImage(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return ApiResult.success(fileResourceService.uploadImage(file, uploaderId));
    }

    @Operation(summary = "文件详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('file:detail')")
    public ApiResult<FileResourceVO> getFile(@NotNull(message = "文件ID不能为空") @PathVariable Long id) {
        return ApiResult.success(fileResourceService.getFile(id));
    }

    @Operation(summary = "读取文件内容")
    @GetMapping("/{id}/content")
    @PreAuthorize("hasAuthority('file:detail')")
    public ResponseEntity<byte[]> getFileContent(@NotNull(message = "文件ID不能为空") @PathVariable Long id) {
        FileContentVO file = fileResourceService.getFileContent(id);
        String filename = file.getOriginalName() == null ? "file" : file.getOriginalName();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String contentType = file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                .body(file.getContent());
    }
}
