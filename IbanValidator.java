package com.pkf.validation.accounts.validator;

import com.pkf.validation.accounts.annotation.ValidIban;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    // Ajout d'une Regex pour vérifier le format des caractères.
    // [A-Z]{2}: Code Pays
    // [0-9]{2}: Clé de contrôle IBAN
    // [A-Z0-9]{4,30}: BBAN (Min 4, Max 30 caractères. Total Max 34)
    private static final Pattern IBAN_FORMAT_PATTERN =
            Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}$");

    @Override
    public boolean isValid(String iban, ConstraintValidatorContext context) {
        if (iban == null || iban.trim().isEmpty()) {
            return true;
        }

        String cleanIban = iban.replaceAll("\\s", "").toUpperCase();

        // 1. Vérification de la plage de longueur ISO globale (15 à 34)
        if (cleanIban.length() < 15 || cleanIban.length() > 34) {
            return false;
        }

        // 2. Vérification du format alphanumérique des caractères
        // (Étape cruciale qui manque dans votre version actuelle)
        if (!IBAN_FORMAT_PATTERN.matcher(cleanIban).matches()) {
            return false;
        }

        // 3. Vérification de la clé de contrôle Modulo 97
        return isValidIbanChecksum(cleanIban);
    }

    // ... (Reste des méthodes isValidIbanChecksum et calculateMod97 inchangées)
    private boolean isValidIbanChecksum(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numericString = new StringBuilder();

        for (char c : rearranged.toCharArray()) {
            if (Character.isDigit(c)) {
                numericString.append(c);
            } else {
                // Conversion A=10, B=11, ...
                numericString.append(c - 'A' + 10);
            }
        }

        return calculateMod97(numericString.toString()) == 1;
    }

    private int calculateMod97(String numericString) {
        int remainder = 0;

        for (int i = 0; i < numericString.length(); i += 9) {
            String block = numericString.substring(i, Math.min(i + 9, numericString.length()));
            // Simule l'opération modulo sur le grand nombre
            remainder = Integer.parseInt(remainder + block) % 97;
        }

        return remainder;
    }
}