package com.example.stagemgmt.security;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

import java.util.Base64;

/** Enveloppe la librairie TOTP pour que le reste de l'appli n'ait jamais à la
 *  connaître directement. Compatible Google Authenticator / Microsoft Authenticator /
 *  toute appli TOTP standard - pas besoin de serveur mail, tout se passe en local. */
@Service
public class TwoFactorService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String genererSecret() {
        return secretGenerator.generate();
    }

    /** QR code prêt à afficher (data URI) pour que le stagiaire/responsable le scanne
     *  avec son appli d'authentification. */
    public String genererQrCode(String username, String secret) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer("InternBridge")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] imageBytes = qrGenerator.generate(data);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + base64;
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Impossible de générer le QR code 2FA", e);
        }
    }

    public boolean verifierCode(String secret, String code) {
        return secret != null && code != null && !code.isBlank() && codeVerifier.isValidCode(secret, code.trim());
    }
}
