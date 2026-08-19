package com.example.smartcustomerservice.controller.conversation;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.dto.ConversationCreateRequest;
import com.example.smartcustomerservice.domain.dto.ConversationMessageCreateRequest;
import com.example.smartcustomerservice.domain.vo.FileContentVO;
import com.example.smartcustomerservice.domain.vo.ConversationMessageVO;
import com.example.smartcustomerservice.domain.vo.ConversationSessionVO;
import com.example.smartcustomerservice.domain.vo.FileResourceVO;
import com.example.smartcustomerservice.service.conversation.ConversationService;
import com.example.smartcustomerservice.service.file.FileResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "用户端会话测试")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/customer-test/conversations")
public class CustomerTestConversationController {

    private final ConversationService conversationService;
    private final FileResourceService fileResourceService;

    public CustomerTestConversationController(ConversationService conversationService,
                                              FileResourceService fileResourceService) {
        this.conversationService = conversationService;
        this.fileResourceService = fileResourceService;
    }

    @Operation(summary = "用户端创建会话")
    @PostMapping
    public ApiResult<ConversationSessionVO> createSession(@Valid @RequestBody ConversationCreateRequest request) {
        return ApiResult.success(conversationService.createSession(request));
    }

    @Operation(summary = "用户端会话详情")
    @GetMapping("/{id}")
    public ApiResult<ConversationSessionVO> getSession(@NotNull(message = "会话ID不能为空") @PathVariable Long id) {
        return ApiResult.success(conversationService.getSession(id));
    }

    @Operation(summary = "用户端消息列表")
    @GetMapping("/{id}/messages")
    public ApiResult<List<ConversationMessageVO>> listMessages(@NotNull(message = "会话ID不能为空") @PathVariable Long id) {
        return ApiResult.success(conversationService.listMessages(id));
    }

    @Operation(summary = "用户端发送消息")
    @PostMapping("/{id}/messages")
    public ApiResult<ConversationMessageVO> sendMessage(@NotNull(message = "会话ID不能为空") @PathVariable Long id,
                                                        @Valid @RequestBody ConversationMessageCreateRequest request) {
        request.setSenderType("CUSTOMER");
        return ApiResult.success(conversationService.sendMessage(id, request));
    }

    @Operation(summary = "用户端上传图片")
    @PostMapping(value = "/files/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<FileResourceVO> uploadImage(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return ApiResult.success(fileResourceService.uploadImage(file, uploaderId));
    }

    @Operation(summary = "用户端上传文件")
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<FileResourceVO> uploadFile(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "uploaderId", required = false) Long uploaderId) {
        return ApiResult.success(fileResourceService.uploadFile(file, uploaderId));
    }

    @Operation(summary = "用户端读取文件内容")
    @GetMapping("/files/{id}/content")
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
