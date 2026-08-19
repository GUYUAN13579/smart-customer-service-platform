package com.example.smartcustomerservice.service.file;

import com.example.smartcustomerservice.domain.vo.FileContentVO;
import com.example.smartcustomerservice.domain.vo.FileResourceVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileResourceService {

    FileResourceVO uploadFile(MultipartFile file, Long uploaderId);

    FileResourceVO uploadImage(MultipartFile file, Long uploaderId);

    FileResourceVO getFile(Long id);

    FileContentVO getFileContent(Long id);
}
