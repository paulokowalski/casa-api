package com.kowalski.casaapi.crescimento.foto;

import com.kowalski.casaapi.crescimento.comum.CrescimentoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
public class LinkAssinado {

    public record Assinatura(long exp, String sig) {
    }

    private final byte[] segredo;
    private final long bloco;
    private final Clock relogio;

    public LinkAssinado(CrescimentoProperties props, Clock relogio) {
        this.relogio = relogio;
        this.bloco = props.linkValidade().toSeconds();
        String s = props.linkSegredo();
        if (s == null || s.length() < 32) {
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.segredo = aleatorio;
            log.warn("crescimento.link-segredo ausente ou curto: usando chave aleatória (links das fotos mudam a cada reinício)");
        } else {
            this.segredo = s.getBytes(StandardCharsets.UTF_8);
        }
    }

    public Assinatura assinar(UUID fotoId, Variante v) {
        long agora = relogio.instant().getEpochSecond();
        long exp = (agora / bloco + 2) * bloco;
        return new Assinatura(exp, hmac(fotoId, v, exp));
    }

    public Instant vencimento(Assinatura a) {
        return Instant.ofEpochSecond(a.exp());
    }

    public boolean valido(UUID fotoId, Variante v, long exp, String sig) {
        if (sig == null || exp < relogio.instant().getEpochSecond()) return false;
        return MessageDigest.isEqual(hmac(fotoId, v, exp).getBytes(StandardCharsets.US_ASCII), sig.getBytes(StandardCharsets.US_ASCII));
    }

    private String hmac(UUID fotoId, Variante v, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            byte[] h = mac.doFinal((fotoId + "." + v.name() + "." + exp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
