package com.fafa.biz.interfaces.http;

import com.fafa.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/biz")
public class SystemController {
    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("fafa-biz is UP");
    }
}
