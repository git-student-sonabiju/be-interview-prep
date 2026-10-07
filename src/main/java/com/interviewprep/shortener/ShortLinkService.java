package com.interviewprep.shortener;

import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.shortener.dto.LinkStatsResponse;
import com.interviewprep.shortener.dto.ShortenRequest;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final ShortLinkRepository shortLinkRepository;
    private final CodeGenerator codeGenerator;
    private final Clock clock;

    @Transactional
    public ShortLink shorten(ShortenRequest request) {
        ShortLink link = new ShortLink();
        link.setCode(uniqueCode());
        link.setOriginalUrl(request.url());
        link.setExpiresAt(request.expiresAt());
        return shortLinkRepository.save(link);
    }

    @Transactional
    public String resolveAndCountVisit(String code) {
        ShortLink link = findByCode(code);
        if (link.isExpired(clock.instant())) {
            throw new LinkExpiredException(code);
        }
        shortLinkRepository.incrementVisitCount(link.getId());
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse stats(String code) {
        return LinkStatsResponse.from(findByCode(code));
    }

    private ShortLink findByCode(String code) {
        return shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Short link", code));
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.next();
            if (!shortLinkRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }
}
