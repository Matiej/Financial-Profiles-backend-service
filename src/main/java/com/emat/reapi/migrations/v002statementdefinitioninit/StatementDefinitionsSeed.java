package com.emat.reapi.migrations.v002statementdefinitioninit;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementProfile;
import com.emat.reapi.statement.domain.StatementType;
import com.emat.reapi.statement.domain.StatementTypeDefinition;

import java.util.List;
import java.util.stream.Stream;

public class StatementDefinitionsSeed {

    private StatementDefinitionsSeed() {
    }

    // ── PROFIL 1 — Strażniczka Braku (pytania 1–9) ──────────────────────────

    public static final List<StatementDefinition> PROFIL_1 = List.of(
            def("1", StatementProfile.PROFIL_1, "p1_q1",
                    "Zawsze mam za mało, żeby odkładać.",
                    "Odkładam nawet małe kwoty i widzę, jak rosną."),
            def("2", StatementProfile.PROFIL_1, "p1_q2",
                    "„Pieniądze nie rosną na drzewach“.",
                    "Pieniądze przychodzą do mnie na wiele sposobów."),
            def("3", StatementProfile.PROFIL_1, "p1_q3",
                    "Pieniądze są powodem konfliktów.",
                    "Pieniądze wspierają harmonię i współpracę."),
            def("4", StatementProfile.PROFIL_1, "p1_q4",
                    "Bogactwo nie jest dla mnie.",
                    "Bogactwo też jest dla mnie."),
            def("5", StatementProfile.PROFIL_1, "p1_q5",
                    "Oszczędzanie to ciągłe wyrzeczenia.",
                    "Oszczędzanie i inwestowanie dają mi wolność."),
            def("6", StatementProfile.PROFIL_1, "p1_q6",
                    "Nie rozmawia się o pieniądzach.",
                    "Swobodnie rozmawiam o pieniądzach."),
            def("7", StatementProfile.PROFIL_1, "p1_q7",
                    "Lepiej ukrywać pieniądze, żeby ich nie stracić.",
                    "Pokazuję, że mam pieniądze i czuję się z tym bezpiecznie."),
            def("8", StatementProfile.PROFIL_1, "p1_q8",
                    "Pieniądze szczęścia nie dają.",
                    "Pieniądze wspierają moje szczęście i spokój."),
            def("9", StatementProfile.PROFIL_1, "p1_q9",
                    "Zawsze, gdy zaczynam mieć więcej pieniędzy, zaraz coś się dzieje i muszę je wydać.",
                    "Nawet gdy życie zaskakuje, czuję się bezpiecznie finansowo.")
    );

    // ── PROFIL 2 — Samowystarczalna Tarcza (pytania 10–18) ──────────────────

    public static final List<StatementDefinition> PROFIL_2 = List.of(
            def("10", StatementProfile.PROFIL_2, "p2_q1",
                    "Nie potrzebuję nikogo, sama wszystko ogarnę.",
                    "Przyjmuję wsparcie i pozostaję niezależna."),
            def("11", StatementProfile.PROFIL_2, "p2_q2",
                    "Trudno mi przyjąć pomoc bez poczucia, że muszę coś dać w zamian.",
                    "Przyjmuję pomoc bez poczucia winy."),
            def("12", StatementProfile.PROFIL_2, "p2_q3",
                    "Muszę wszystko zrobić sama, żeby było dobrze.",
                    "Potrafię ufać innym i dzielić się odpowiedzialnością."),
            def("13", StatementProfile.PROFIL_2, "p2_q4",
                    "Jeśli ktoś mi pomaga, staję się od niego zależna.",
                    "Przyjmuję pomoc i zachowuję niezależność."),
            def("14", StatementProfile.PROFIL_2, "p2_q5",
                    "Prosząc o pomoc, pokazuję słabość.",
                    "Prosząc o pomoc, pokazuję odwagę."),
            def("15", StatementProfile.PROFIL_2, "p2_q6",
                    "Lepiej być niezależną niż ryzykować odrzucenie.",
                    "Jestem niezależna i otwarta na bliskość jednocześnie."),
            def("16", StatementProfile.PROFIL_2, "p2_q7",
                    "Dawanie jest łatwe, przyjmowanie jest bardzo trudne.",
                    "Potrafię przyjmować z taką samą otwartością, z jaką daję."),
            def("17", StatementProfile.PROFIL_2, "p2_q8",
                    "Muszę wszystko zabezpieczyć sama, bo nikomu nie mogę zaufać.",
                    "Liczę na innych i czuję się bezpiecznie."),
            def("18", StatementProfile.PROFIL_2, "p2_q9",
                    "Pytanie o pieniądze to oznaka słabości.",
                    "Pytam o pieniądze i czuję się silna.")
    );

    // ── PROFIL 3 — Zamrożona Wizjonerka (pytania 19–27) ─────────────────────

    public static final List<StatementDefinition> PROFIL_3 = List.of(
            def("19", StatementProfile.PROFIL_3, "p3_q1",
                    "Nie ma sensu zajmować się finansami, dopóki nie ma kryzysu.",
                    "Robię plany finansowe na spokojnie, zanim nadejdzie kryzys."),
            def("20", StatementProfile.PROFIL_3, "p3_q2",
                    "„Zamrażam“ pieniądze i pomysły, bo boję się błędu.",
                    "Działam, nawet jeśli nie jest idealnie."),
            def("21", StatementProfile.PROFIL_3, "p3_q3",
                    "Zarządzanie finansami jest trudne i nieprzyjemne.",
                    "Zarządzam finansami z lekkością i pewnością siebie."),
            def("22", StatementProfile.PROFIL_3, "p3_q4",
                    "Sukces kojarzy mi się z presją i stresem.",
                    "Czuję spokój, odnosząc sukcesy."),
            def("23", StatementProfile.PROFIL_3, "p3_q5",
                    "Nie jestem wystarczająco dobra, żeby zarabiać dużo.",
                    "Jestem wystarczająco dobra, by zarabiać dobrze."),
            def("24", StatementProfile.PROFIL_3, "p3_q6",
                    "Nie ruszę, dopóki nie będę perfekcyjnie przygotowana.",
                    "Działam jak tylko mam pomysł i uczę się w trakcie."),
            def("25", StatementProfile.PROFIL_3, "p3_q7",
                    "Nie ufam swoim decyzjom finansowym i wolę, żeby inni doradzali mi, co robić.",
                    "Mam do siebie pełne zaufanie podejmując decyzje finansowe, jak chcę to korzystam z opinii innych."),
            def("26", StatementProfile.PROFIL_3, "p3_q8",
                    "Lepiej nie zaczynać niż ponieść porażkę.",
                    "Każdy krok daje mi więcej jasności niż bezruch."),
            def("27", StatementProfile.PROFIL_3, "p3_q9",
                    "Popełnianie błędów w zarządzaniu swoimi finansami to porażka.",
                    "Błędy są częścią uczenia się i pomagają mi lepiej zarządzać finansami.")
    );

    // ── PROFIL 4 — Lojalna Dziedziczka (pytania 28–36) ──────────────────────

    public static final List<StatementDefinition> PROFIL_4 = List.of(
            def("28", StatementProfile.PROFIL_4, "p4_q1",
                    "Czuję, że nie mogę mieć więcej niż moi bliscy.",
                    "Nawet gdy mam więcej, nadal czuję więź z bliskimi."),
            def("29", StatementProfile.PROFIL_4, "p4_q2",
                    "Jeśli odniosę sukces, rodzice mogą odebrać to jako zagrożenie.",
                    "Świętuję swój sukces razem z rodziną, bo to nas łączy."),
            def("30", StatementProfile.PROFIL_4, "p4_q3",
                    "Jeśli się wzbogacę, stracę rodzinę lub przyjaciół.",
                    "Wzbogacam się i utrzymuję bliskie relacje."),
            def("31", StatementProfile.PROFIL_4, "p4_q4",
                    "Nie mogę mieć lepiej niż moi rodzice.",
                    "Tworzę nowe wzorce finansowe w swojej rodzinie."),
            def("32", StatementProfile.PROFIL_4, "p4_q5",
                    "Czuję, że muszę dźwigać finansowe ciężary mojej rodziny.",
                    "Oddaję to, co nie jest moje, i zajmuję się własnym życiem."),
            def("33", StatementProfile.PROFIL_4, "p4_q6",
                    "Bieda uszlachetnia.",
                    "Jestem szlachetna i bogata jednocześnie."),
            def("34", StatementProfile.PROFIL_4, "p4_q7",
                    "Boję się, że kiedy mam więcej, ludzie kochają mnie nie za to, kim jestem, tylko za to, co mam.",
                    "Jestem kochana za to, kim jestem, niezależnie od tego, ile mam."),
            def("35", StatementProfile.PROFIL_4, "p4_q8",
                    "W mojej rodzinie wszyscy odnieśli sukces finansowy, ja też muszę.",
                    "Podążam własną drogą i mam prawo definiować sukces po swojemu."),
            def("36", StatementProfile.PROFIL_4, "p4_q9",
                    "Nie mogę mieć tego, czego moja mama/babcia nie miała.",
                    "Tworzę nowe, lepsze wzorce finansowe dla swojego pokolenia.")
    );

    // ── PROFIL 5 — Wycofana Liderka (pytania 37–45) ─────────────────────────

    public static final List<StatementDefinition> PROFIL_5 = List.of(
            def("37", StatementProfile.PROFIL_5, "p5_q1",
                    "Ludzie mogą mnie skrzywdzić, gdy zobaczą, że mam pieniądze.",
                    "Bycie bogatą jest bezpieczne."),
            def("38", StatementProfile.PROFIL_5, "p5_q2",
                    "Mój sukces finansowy wywołuje zazdrość innych.",
                    "Sukces finansowy może budzić inspirację i szacunek."),
            def("39", StatementProfile.PROFIL_5, "p5_q3",
                    "Kiedy okazuję zaufanie, ludzie to wykorzystują.",
                    "Mogę ufać i zachować swoje granice."),
            def("40", StatementProfile.PROFIL_5, "p5_q4",
                    "Im więcej mam, tym bardziej jestem narażona na ataki.",
                    "Mogę mieć dużo i czuć się chroniona."),
            def("41", StatementProfile.PROFIL_5, "p5_q5",
                    "Sukces finansowy to samotność.",
                    "Mogę cieszyć się sukcesem finansowym, otoczona ludźmi, których lubię."),
            def("42", StatementProfile.PROFIL_5, "p5_q6",
                    "Pokazanie, że mam więcej, sprowokuje ataki.",
                    "Mogę mieć więcej i czuć się bezpieczna."),
            def("43", StatementProfile.PROFIL_5, "p5_q7",
                    "Jeśli będę bogata, ludzie, których znam, się ode mnie odsuną.",
                    "Mogę mieć więcej i nadal być blisko z ludźmi, którzy są dla mnie ważni."),
            def("44", StatementProfile.PROFIL_5, "p5_q8",
                    "Lepiej się nie wychylać, wtedy będę bezpieczna.",
                    "Mogę być widoczna i nadal czuć się bezpiecznie."),
            def("45", StatementProfile.PROFIL_5, "p5_q9",
                    "Sukces odbierze mi prywatność.",
                    "Mogę mieć sukces i zachować prywatność, której potrzebuję.")
    );

    // ── PROFIL 6 — Zapracowana Perfekcjonistka (pytania 46–54) ──────────────

    public static final List<StatementDefinition> PROFIL_6 = List.of(
            def("46", StatementProfile.PROFIL_6, "p6_q1",
                    "Na pieniądze trzeba ciężko pracować i się poświęcać.",
                    "Zarabiam pieniądze w lekki i przyjemny sposób."),
            def("47", StatementProfile.PROFIL_6, "p6_q2",
                    "Nie mogę podnieść cen, bo stracę klientów.",
                    "Podnoszę ceny i nadal mam klientów."),
            def("48", StatementProfile.PROFIL_6, "p6_q3",
                    "Zawsze muszę być produktywna, czuję się winna, gdy nie robię wystarczająco.",
                    "Potrafię odpoczywać bez poczucia winy."),
            def("49", StatementProfile.PROFIL_6, "p6_q4",
                    "Wypalenie jest ceną sukcesu.",
                    "Sukces dodaje energii."),
            def("50", StatementProfile.PROFIL_6, "p6_q5",
                    "Zarabianie to stres.",
                    "Zarabianie jest przyjemne i daje mi satysfakcję."),
            def("51", StatementProfile.PROFIL_6, "p6_q6",
                    "Trzeba mieć „fach w ręku“, żeby zarabiać.",
                    "Zarabiam dzięki swoim talentom i pomysłom."),
            def("52", StatementProfile.PROFIL_6, "p6_q7",
                    "Muszę wyglądać, jakbym wszystko miała pod kontrolą.",
                    "Nie muszę być idealna, żeby zasługiwać na szacunek."),
            def("53", StatementProfile.PROFIL_6, "p6_q8",
                    "Na przyjemność trzeba zasłużyć.",
                    "Cieszę się przyjemnością bez poczucia winy."),
            def("54", StatementProfile.PROFIL_6, "p6_q9",
                    "Jeśli ja tego nie udźwignę, wszystko się zawali.",
                    "Świat może działać dobrze, nawet gdy ja odpoczywam.")
    );

    // ── PROFIL 7 — Zatrzymana w Przyjmowaniu (pytania 55–63) ────────────────

    public static final List<StatementDefinition> PROFIL_7 = List.of(
            def("55", StatementProfile.PROFIL_7, "p7_q1",
                    "Moja praca jest warta mniej niż dostaję.",
                    "Moja praca jest warta co najmniej tyle, ile dostaję."),
            def("56", StatementProfile.PROFIL_7, "p7_q2",
                    "Nie powinno się brać pieniędzy za pracę z energią/duchowością.",
                    "Pomagam innym na poziomie energetycznym i duchowym i dobrze zarabiam."),
            def("57", StatementProfile.PROFIL_7, "p7_q3",
                    "Trudno mi przyjąć coś bez poczucia, że powinnam się odwdzięczyć.",
                    "Przyjmuję z wdzięcznością, bez obowiązku rewanżu."),
            def("58", StatementProfile.PROFIL_7, "p7_q4",
                    "Nie wypada przyjmować pieniędzy, jeśli na nie nie zapracowałam.",
                    "Przyjmuję pieniądze z wdzięcznością, nawet gdy nie były wynikiem mojego wysiłku."),
            def("59", StatementProfile.PROFIL_7, "p7_q5",
                    "Przyjmowanie prezentów lub pieniędzy jest krępujące.",
                    "Przyjmowanie prezentów i pieniędzy jest powodem do wdzięczności i radości."),
            def("60", StatementProfile.PROFIL_7, "p7_q6",
                    "Ludzie lubią mnie tylko, gdy im coś daję.",
                    "Jestem lubiana i kochana za to, kim jestem."),
            def("61", StatementProfile.PROFIL_7, "p7_q7",
                    "Nie wypada brać pieniędzy za coś, co przychodzi mi z łatwością.",
                    "Zarabianie na tym, co przychodzi z lekkością i radością, jest dobre."),
            def("62", StatementProfile.PROFIL_7, "p7_q8",
                    "W moim wieku lepiej zainwestować w coś konkretnego albo w dzieci, niż w siebie.",
                    "Zawsze warto inwestować w siebie, niezależnie od wieku i etapu życia."),
            def("63", StatementProfile.PROFIL_7, "p7_q9",
                    "Pozwalanie sobie na luksus to przesada.",
                    "Potrafię cieszyć się luksusem bez poczucia winy.")
    );

    // ── PROFIL 8 — Idealistka Skromności (pytania 64–72) ────────────────────

    public static final List<StatementDefinition> PROFIL_8 = List.of(
            def("64", StatementProfile.PROFIL_8, "p8_q1",
                    "Bieda jest cnotą.",
                    "Posiadanie pieniędzy nie wyklucza bycia dobrym człowiekiem."),
            def("65", StatementProfile.PROFIL_8, "p8_q2",
                    "Cieszenie się pieniędzmi świadczy o braku pokory.",
                    "Cieszenie się pieniędzmi i bycie pokorną nie wykluczają się."),
            def("66", StatementProfile.PROFIL_8, "p8_q3",
                    "Pragnienie bogactwa jest złe.",
                    "Pragnę bogactwa mając czyste intencje."),
            def("67", StatementProfile.PROFIL_8, "p8_q4",
                    "Kiedy ludzie mają za dużo, to się zmieniają na gorsze.",
                    "Posiadanie dużych pieniędzy nie oznacza, że człowiek zmieni się na gorsze."),
            def("68", StatementProfile.PROFIL_8, "p8_q5",
                    "Tylko skromne życie jest wartościowe.",
                    "Wartość życia nie zależy od tego, ile mam."),
            def("69", StatementProfile.PROFIL_8, "p8_q6",
                    "Bogactwo często psuje ludzi.",
                    "Bogactwo może pokazywać to, co w człowieku dobre."),
            def("70", StatementProfile.PROFIL_8, "p8_q7",
                    "Nie wypada mieć więcej niż inni.",
                    "Mam więcej niż inni i nadal jestem dobrym człowiekiem."),
            def("71", StatementProfile.PROFIL_8, "p8_q8",
                    "Pieniądze oddzielają ludzi od siebie.",
                    "Pieniądze mogą wspierać relacje i bliskość."),
            def("72", StatementProfile.PROFIL_8, "p8_q9",
                    "Posiadanie dużych pieniędzy oddziela od duchowości.",
                    "Pieniądze mogą wspierać mój rozwój duchowy.")
    );

    public static final List<StatementDefinition> ALL = Stream.of(
            PROFIL_1, PROFIL_2, PROFIL_3, PROFIL_4,
            PROFIL_5, PROFIL_6, PROFIL_7, PROFIL_8
    ).flatMap(List::stream).toList();

    private static StatementDefinition def(String id, StatementProfile profile, String key,
                                           String limiting, String supporting) {
        return new StatementDefinition(
                id,
                profile,
                key,
                List.of(
                        new StatementTypeDefinition(StatementType.LIMITING, limiting),
                        new StatementTypeDefinition(StatementType.SUPPORTING, supporting)
                )
        );
    }
}
