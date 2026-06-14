package com.emat.reapi.statement.domain;

public enum StatementProfile {

    PROFIL_1(
            "Strażniczka Braku",
            "Strażniczka Braku (blokada)",
            "Strażniczka Braku (strefa przejściowa)",
            "Zakorzeniona w Obfitości (zasoby)"
    ),
    PROFIL_2(
            "Samowystarczalna Tarcza",
            "Samowystarczalna Tarcza (blokada)",
            "Samowystarczalna Tarcza (strefa przejściowa)",
            "Silna z Wyboru (zasoby)"
    ),
    PROFIL_3(
            "Zamrożona Wizjonerka",
            "Zamrożona Wizjonerka (blokada)",
            "Zamrożona Wizjonerka (strefa przejściowa)",
            "Wizjonerka w Działaniu (zasoby)"
    ),
    PROFIL_4(
            "Lojalna Dziedziczka",
            "Lojalna Dziedziczka (blokada)",
            "Lojalna Dziedziczka (strefa przejściowa)",
            "Wyzwolona Dziedziczka (zasoby)"
    ),
    PROFIL_5(
            "Wycofana Liderka",
            "Wycofana Liderka (blokada)",
            "Wycofana Liderka (strefa przejściowa)",
            "Inspirująca Liderka (zasoby)"
    ),
    PROFIL_6(
            "Zapracowana Perfekcjonistka",
            "Zapracowana Perfekcjonistka (blokada)",
            "Zapracowana Perfekcjonistka (strefa przejściowa)",
            "Mistrzyni Równowagi (zasoby)"
    ),
    PROFIL_7(
            "Zatrzymana w Przyjmowaniu",
            "Zatrzymana w Przyjmowaniu (blokada)",
            "Zatrzymana w Przyjmowaniu (strefa przejściowa)",
            "Otwarta na Obfitość (zasoby)"
    ),
    PROFIL_8(
            "Idealistka Skromności",
            "Idealistka Skromności (blokada)",
            "Idealistka Skromności (strefa przejściowa)",
            "Hojna Idealistka (zasoby)"
    );

    private final String plName;
    private final String blockingName;
    private final String transitionalName;
    private final String resourcesName;

    StatementProfile(String plName, String blockingName, String transitionalName, String resourcesName) {
        this.plName = plName;
        this.blockingName = blockingName;
        this.transitionalName = transitionalName;
        this.resourcesName = resourcesName;
    }

    public String getPlName() {
        return plName;
    }

    public String computeLabel(double percent) {
        if (percent <= 0) return blockingName;
        if (percent < 68) return transitionalName;
        return resourcesName;
    }

    public static double computePercent(int totalScore, int totalAnswers) {
        if (totalAnswers == 0) return 0;
        return (double) totalScore / (totalAnswers * 2) * 100;
    }
}
