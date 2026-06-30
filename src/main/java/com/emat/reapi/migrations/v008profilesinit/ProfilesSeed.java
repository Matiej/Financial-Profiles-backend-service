package com.emat.reapi.migrations.v008profilesinit;

import com.emat.reapi.statement.domain.Profile;

import java.time.Instant;
import java.util.List;

/**
 * Seeds the 8 initial profiles (carried over from the former StatementProfile enum).
 * Id = stable slug "profil_1".."profil_8" — referenced by definitions (F3+).
 */
public class ProfilesSeed {

    private ProfilesSeed() {
    }

    public static final List<Profile> ALL = List.of(
            profile("profil_1", 1, "Strażniczka Braku",
                    "Strażniczka Braku (blokada)",
                    "Strażniczka Braku (strefa przejściowa)",
                    "Zakorzeniona w Obfitości (zasoby)"),
            profile("profil_2", 2, "Samowystarczalna Tarcza",
                    "Samowystarczalna Tarcza (blokada)",
                    "Samowystarczalna Tarcza (strefa przejściowa)",
                    "Silna z Wyboru (zasoby)"),
            profile("profil_3", 3, "Zamrożona Wizjonerka",
                    "Zamrożona Wizjonerka (blokada)",
                    "Zamrożona Wizjonerka (strefa przejściowa)",
                    "Wizjonerka w Działaniu (zasoby)"),
            profile("profil_4", 4, "Lojalna Dziedziczka",
                    "Lojalna Dziedziczka (blokada)",
                    "Lojalna Dziedziczka (strefa przejściowa)",
                    "Wyzwolona Dziedziczka (zasoby)"),
            profile("profil_5", 5, "Wycofana Liderka",
                    "Wycofana Liderka (blokada)",
                    "Wycofana Liderka (strefa przejściowa)",
                    "Inspirująca Liderka (zasoby)"),
            profile("profil_6", 6, "Zapracowana Perfekcjonistka",
                    "Zapracowana Perfekcjonistka (blokada)",
                    "Zapracowana Perfekcjonistka (strefa przejściowa)",
                    "Mistrzyni Równowagi (zasoby)"),
            profile("profil_7", 7, "Zatrzymana w Przyjmowaniu",
                    "Zatrzymana w Przyjmowaniu (blokada)",
                    "Zatrzymana w Przyjmowaniu (strefa przejściowa)",
                    "Otwarta na Obfitość (zasoby)"),
            profile("profil_8", 8, "Idealistka Skromności",
                    "Idealistka Skromności (blokada)",
                    "Idealistka Skromności (strefa przejściowa)",
                    "Hojna Idealistka (zasoby)")
    );

    private static Profile profile(String id, int order, String plName,
                                   String blockingName, String transitionalName, String resourcesName) {
        Instant now = Instant.now();
        return Profile.builder()
                .id(id)
                .plName(plName)
                .blockingName(blockingName)
                .transitionalName(transitionalName)
                .resourcesName(resourcesName)
                .order(order)
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
