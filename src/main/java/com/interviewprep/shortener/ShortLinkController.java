package com.interviewprep.shortener;

import com.interviewprep.shortener.dto.LinkStatsResponse;
import com.interviewprep.shortener.dto.ShortLinkResponse;
import com.interviewprep.shortener.dto.ShortenRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping("/api/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ShortLinkResponse shorten(@Valid @RequestBody ShortenRequest request) {
        ShortLink link = shortLinkService.shorten(request);
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/s/{code}")
                .buildAndExpand(link.getCode())
                .toUriString();
        return new ShortLinkResponse(link.getCode(), shortUrl, link.getOriginalUrl(), link.getExpiresAt(), link.getCreatedAt());
    }

    @GetMapping("/s/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = shortLinkService.resolveAndCountVisit(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    @GetMapping("/api/links/{code}/stats")
    public LinkStatsResponse stats(@PathVariable String code) {
        return shortLinkService.stats(code);
    }
}
