package com.example.smartcustomerservice.service.impl.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.KnowledgeDocumentCreateRequest;
import com.example.smartcustomerservice.domain.dto.KnowledgeDocumentQueryRequest;
import com.example.smartcustomerservice.domain.entity.KnowledgeDocument;
import com.example.smartcustomerservice.domain.vo.FileContentVO;
import com.example.smartcustomerservice.domain.vo.FileResourceVO;
import com.example.smartcustomerservice.domain.vo.KnowledgeDocumentVO;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeDocumentMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.file.FileResourceService;
import com.example.smartcustomerservice.service.knowledge.KnowledgeDocumentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {
    private static final Set<String> ALLOWED_TEXT_EXTENSIONS =
            Set.of("txt", "md", "csv", "json");
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final FileResourceService fileResourceService;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                        FileResourceService fileResourceService) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.fileResourceService = fileResourceService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO createDocument(KnowledgeDocumentCreateRequest request) {
        FileResourceVO fileResourceVO = fileResourceService.getFile(request.getFileId());
        if(!isAllowedPlainTextFile(fileResourceVO))
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件类型不支持");
        Long documentCount = knowledgeDocumentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getFileId, request.getFileId()));
        if (documentCount != null && documentCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该文件已导入知识库");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取当前导入人");
        }
        KnowledgeDocument knowledgeDocument = new KnowledgeDocument();
        knowledgeDocument.setDocumentName(fileResourceVO.getOriginalName());
        knowledgeDocument.setFileId(request.getFileId());
        knowledgeDocument.setContentType(fileResourceVO.getContentType());
        knowledgeDocument.setFileSize(fileResourceVO.getFileSize());
        knowledgeDocument.setStatus("UPLOADED");
        knowledgeDocument.setCreatedAt(LocalDateTime.now());
        knowledgeDocument.setDeleted(0);
        knowledgeDocument.setUpdatedAt(LocalDateTime.now());
        knowledgeDocument.setCreatedBy(currentUserId);
        int inserted = knowledgeDocumentMapper.insert(knowledgeDocument);
        if(inserted != 1)
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "导入失败");
        KnowledgeDocumentVO knowledgeDocumentVO = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(knowledgeDocument, knowledgeDocumentVO);
        return knowledgeDocumentVO;

    }

    @Override
    public PageResult<KnowledgeDocumentVO> pageDocuments(KnowledgeDocumentQueryRequest request) {
        KnowledgeDocumentQueryRequest safeRequest = request == null
                ? new KnowledgeDocumentQueryRequest()
                : request;
        Page<KnowledgeDocument> page = knowledgeDocumentMapper.selectPage(
                new Page<>(safeRequest.getPage(), safeRequest.getSize()),
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getDeleted, 0)
                        .like(StringUtils.hasText(safeRequest.getKeyword()),
                                KnowledgeDocument::getDocumentName, safeRequest.getKeyword())
                        .eq(StringUtils.hasText(safeRequest.getStatus()),
                                KnowledgeDocument::getStatus, safeRequest.getStatus())
                        .orderByDesc(KnowledgeDocument::getCreatedAt)
                        .orderByDesc(KnowledgeDocument::getId)
        );
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public KnowledgeDocumentVO getDocument(Long id) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(id);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在");
        }
        return toVO(document);
    }

    @Override
    public KnowledgeDocumentVO parseDocument(Long id) {
        KnowledgeDocument knowledgeDocument = getActiveDocument(id);
        if (!"UPLOADED".equals(knowledgeDocument.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有待解析的文件可以执行解析");
        }
        return doParse(knowledgeDocument, "UPLOADED");
    }

    @Override
    public KnowledgeDocumentVO retryParseDocument(Long id) {
        KnowledgeDocument knowledgeDocument = getActiveDocument(id);
        if (!"FAILED".equals(knowledgeDocument.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有解析失败的文件可以重试");
        }
        return doParse(knowledgeDocument, "FAILED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO offlineDocument(Long id) {
        KnowledgeDocument document = getActiveDocument(id);
        if ("PARSING".equals(document.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "文件正在解析，暂不能下线");
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = knowledgeDocumentMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getId, id)
                .eq(KnowledgeDocument::getDeleted, 0)
                .ne(KnowledgeDocument::getStatus, "PARSING")
                .set(KnowledgeDocument::getStatus, "OFFLINE")
                .set(KnowledgeDocument::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "文件状态已变化，请刷新后重试");
        }
        document.setStatus("OFFLINE");
        document.setUpdatedAt(now);
        return toVO(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDocument(Long id) {
        KnowledgeDocument document = getActiveDocument(id);
        if ("PARSING".equals(document.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "文件正在解析，暂不能删除");
        }
        int updated = knowledgeDocumentMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getId, id)
                .eq(KnowledgeDocument::getDeleted, 0)
                .ne(KnowledgeDocument::getStatus, "PARSING")
                .set(KnowledgeDocument::getDeleted, 1)
                .set(KnowledgeDocument::getStatus, "OFFLINE")
                .set(KnowledgeDocument::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "文件状态已变化，请刷新后重试");
        }
        return true;
    }




    private boolean isAllowedPlainTextFile(FileResourceVO file) {
        if (file == null || !StringUtils.hasText(file.getOriginalName())) {
            return false;
        }

        String fileName = file.getOriginalName().toLowerCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(dotIndex + 1);
        return ALLOWED_TEXT_EXTENSIONS.contains(extension);
    }

    private KnowledgeDocumentVO doParse(KnowledgeDocument document, String expectedStatus) {
        LocalDateTime now = LocalDateTime.now();
        int processingUpdated = knowledgeDocumentMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getId, document.getId())
                .eq(KnowledgeDocument::getDeleted, 0)
                .eq(KnowledgeDocument::getStatus, expectedStatus)
                .set(KnowledgeDocument::getStatus, "PARSING")
                .set(KnowledgeDocument::getErrorMessage, null)
                .set(KnowledgeDocument::getUpdatedAt, now));
        if (processingUpdated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "文件解析状态已变化，请刷新后重试");
        }

        try {
            FileResourceVO fileResource = fileResourceService.getFile(document.getFileId());
            if (!isAllowedPlainTextFile(fileResource)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "当前文件不是支持解析的纯文本格式");
            }
            FileContentVO fileContent = fileResourceService.getFileContent(document.getFileId());
            if (fileContent == null || fileContent.getContent() == null || fileContent.getContent().length == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "文件内容为空，无法解析");
            }

            String extractedContent = new String(fileContent.getContent(), StandardCharsets.UTF_8);
            int parsedUpdated = knowledgeDocumentMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocument>()
                    .eq(KnowledgeDocument::getId, document.getId())
                    .eq(KnowledgeDocument::getDeleted, 0)
                    .eq(KnowledgeDocument::getStatus, "PARSING")
                    .set(KnowledgeDocument::getExtractedContent, extractedContent)
                    .set(KnowledgeDocument::getParserType, "PLAIN_TEXT")
                    .set(KnowledgeDocument::getStatus, "PARSED")
                    .set(KnowledgeDocument::getErrorMessage, null)
                    .set(KnowledgeDocument::getUpdatedAt, LocalDateTime.now()));
            if (parsedUpdated != 1) {
                throw new BusinessException(ResultCode.CONFLICT, "文件解析状态已变化，请刷新后重试");
            }

            document.setExtractedContent(extractedContent);
            document.setParserType("PLAIN_TEXT");
            document.setStatus("PARSED");
            document.setErrorMessage(null);
            document.setUpdatedAt(LocalDateTime.now());
            return toVO(document);
        } catch (Exception e) {
            knowledgeDocumentMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocument>()
                    .eq(KnowledgeDocument::getId, document.getId())
                    .eq(KnowledgeDocument::getDeleted, 0)
                    .eq(KnowledgeDocument::getStatus, "PARSING")
                    .set(KnowledgeDocument::getStatus, "FAILED")
                    .set(KnowledgeDocument::getErrorMessage, truncateError(e.getMessage()))
                    .set(KnowledgeDocument::getUpdatedAt, LocalDateTime.now()));
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文件解析失败");
        }
    }

    private KnowledgeDocument getActiveDocument(Long id) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(id);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库文件不存在");
        }
        return document;
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知解析异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private KnowledgeDocumentVO toVO(KnowledgeDocument document) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(document, vo);
        return vo;
    }
}
