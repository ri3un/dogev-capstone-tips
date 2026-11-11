package com.gaebalhagae.report.service;

import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.report.dto.ReportData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Thymeleaf(templates/report.html) → XHTML → Flying Saucer PDF.
 * 한글이 깨지지 않도록 Pretendard TTF 를 PDF 에 넣습니다. 폰트는 jar 안에 있어 한 번 임시 파일로 꺼내 씁니다.
 */
@Slf4j
@Component
public class PdfRenderer {

    private static final String[] FONTS = {"fonts/Pretendard-Regular.ttf", "fonts/Pretendard-Bold.ttf"};

    private final TemplateEngine templateEngine;
    private final List<String> fontPaths = new ArrayList<>();

    public PdfRenderer(TemplateEngine templateEngine) throws IOException {
        this.templateEngine = templateEngine;
        Path dir = Files.createTempDirectory("kungkung-fonts");
        for (String font : FONTS) {
            Path target = dir.resolve(Path.of(font).getFileName());
            try (InputStream in = new ClassPathResource(font).getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            target.toFile().deleteOnExit();
            fontPaths.add(target.toString());
        }
    }

    public byte[] render(ReportData data) {
        Context ctx = new Context();
        ctx.setVariable("r", data);
        String html = templateEngine.process("report", ctx);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            for (String path : fontPaths) {
                renderer.getFontResolver().addFont(path, "Identity-H", true);
            }
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF를 만들지 못했습니다");
        }
    }
}
