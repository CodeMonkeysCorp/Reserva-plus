package com.reservaplus.reserva_plus.support;

import java.util.Locale;
import java.util.Objects;

public final class EmailAddressSupport {

    private EmailAddressSupport() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean sameEmail(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }
}
