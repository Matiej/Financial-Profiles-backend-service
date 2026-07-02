package com.emat.reapi.migrations.v002statementdefinitioninit;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementType;
import com.emat.reapi.statement.domain.StatementTypeDefinition;

import java.util.List;
import java.util.stream.Stream;

public class StatementDefinitionsSeed {

    private StatementDefinitionsSeed() {
    }

    // ── PROFIL 1 — Strażniczka Braku (pytania 1–9) ──────────────────────────

    public static final List<StatementDefinition> PROFIL_1 = List.of(
            def("profil_1", "p1_q1",
                    "Zawsze mam za mało, żeby odkładać.",
                    "Odkładam nawet małe kwoty i widzę, jak rosną."),
            def("profil_1", "p1_q2",
                    "„Pieniądze nie rosną na drzewach“.",
                    "Pieniądze przychodzą do mnie na wiele sposobów."),
            def("profil_1", "p1_q3",
                    "Pieniądze są powodem konfliktów.",
                    "Pieniądze wspierają harmonię i współpracę."),
            def("profil_1", "p1_q4",
                    "Bogactwo nie jest dla mnie.",
                    "Bogactwo też jest dla mnie."),
            def("profil_1", "p1_q5",
                    "Oszczędzanie to ciągłe wyrzeczenia.",
                    "Oszczędzanie i inwestowanie dają mi wolność."),
            def("profil_1", "p1_q6",
                    "Nie rozmawia się o pieniądzach.",
                    "Swobodnie rozmawiam o pieniądzach."),
            def("profil_1", "p1_q7",
                    "Lepiej ukrywać pieniądze, żeby ich nie stracić.",
                    "Pokazuję, że mam pieniądze i czuję się z tym bezpiecznie."),
            def("profil_1", "p1_q8",
                    "Pieniądze szczęścia nie dają.",
                    "Pieniądze wspierają moje szczęście i spokój."),
            def("profil_1", "p1_q9",
                    "Zawsze, gdy zaczynam mieć więcej pieniędzy, zaraz coś się dzieje i muszę je wydać.",
                    "Nawet gdy życie zaskakuje, czuję się bezpiecznie finansowo.")
    );

    // ── PROFIL 2 — Samowystarczalna Tarcza (pytania 10–18) ──────────────────

    public static final List<StatementDefinition> PROFIL_2 = List.of(
            def("profil_2", "p2_q1",
                    "Nie potrzebuję nikogo, sama wszystko ogarnę.",
                    "Przyjmuję wsparcie i pozostaję niezależna."),
            def("profil_2", "p2_q2",
                    "Trudno mi przyjąć pomoc bez poczucia, że muszę coś dać w zamian.",
                    "Przyjmuję pomoc bez poczucia winy."),
            def("profil_2", "p2_q3",
                    "Muszę wszystko zrobić sama, żeby było dobrze.",
                    "Potrafię ufać innym i dzielić się odpowiedzialnością."),
            def("profil_2", "p2_q4",
                    "Jeśli ktoś mi pomaga, staję się od niego zależna.",
                    "Przyjmuję pomoc i zachowuję niezależność."),
            def("profil_2", "p2_q5",
                    "Prosząc o pomoc, pokazuję słabość.",
                    "Prosząc o pomoc, pokazuję odwagę."),
            def("profil_2", "p2_q6",
                    "Lepiej być niezależną niż ryzykować odrzucenie.",
                    "Jestem niezależna i otwarta na bliskość jednocześnie."),
            def("profil_2", "p2_q7",
                    "Dawanie jest łatwe, przyjmowanie jest bardzo trudne.",
                    "Potrafię przyjmować z taką samą otwartością, z jaką daję."),
            def("profil_2", "p2_q8",
                    "Muszę wszystko zabezpieczyć sama, bo nikomu nie mogę zaufać.",
                    "Liczę na innych i czuję się bezpiecznie."),
            def("profil_2", "p2_q9",
                    "Pytanie o pieniądze to oznaka słabości.",
                    "Pytam o pieniądze i czuję się silna.")
    );

    // ── PROFIL 3 — Zamrożona Wizjonerka (pytania 19–27) ─────────────────────

    public static final List<StatementDefinition> PROFIL_3 = List.of(
            def("profil_3", "p3_q1",
                    "Nie ma sensu zajmować się finansami, dopóki nie ma kryzysu.",
                    "Robię plany finansowe na spokojnie, zanim nadejdzie kryzys."),
            def("profil_3", "p3_q2",
                    "„Zamrażam“ pieniądze i pomysły, bo boję się błędu.",
                    "Działam, nawet jeśli nie jest idealnie."),
            def("profil_3", "p3_q3",
                    "Zarządzanie finansami jest trudne i nieprzyjemne.",
                    "Zarządzam finansami z lekkością i pewnością siebie."),
            def("profil_3", "p3_q4",
                    "Sukces kojarzy mi się z presją i stresem.",
                    "Czuję spokój, odnosząc sukcesy."),
            def("profil_3", "p3_q5",
                    "Nie jestem wystarczająco dobra, żeby zarabiać dużo.",
                    "Jestem wystarczająco dobra, by zarabiać dobrze."),
            def("profil_3", "p3_q6",
                    "Nie ruszę, dopóki nie będę perfekcyjnie przygotowana.",
                    "Działam jak tylko mam pomysł i uczę się w trakcie."),
            def("profil_3", "p3_q7",
                    "Nie ufam swoim decyzjom finansowym i wolę, żeby inni doradzali mi, co robić.",
                    "Mam do siebie pełne zaufanie podejmując decyzje finansowe, jak chcę to korzystam z opinii innych."),
            def("profil_3", "p3_q8",
                    "Lepiej nie zaczynać niż ponieść porażkę.",
                    "Każdy krok daje mi więcej jasności niż bezruch."),
            def("profil_3", "p3_q9",
                    "Popełnianie błędów w zarządzaniu swoimi finansami to porażka.",
                    "Błędy są częścią uczenia się i pomagają mi lepiej zarządzać finansami.")
    );

    // ── PROFIL 4 — Lojalna Dziedziczka (pytania 28–36) ──────────────────────

    public static final List<StatementDefinition> PROFIL_4 = List.of(
            def("profil_4", "p4_q1",
                    "Czuję, że nie mogę mieć więcej niż moi bliscy.",
                    "Nawet gdy mam więcej, nadal czuję więź z bliskimi."),
            def("profil_4", "p4_q2",
                    "Jeśli odniosę sukces, rodzice mogą odebrać to jako zagrożenie.",
                    "Świętuję swój sukces razem z rodziną, bo to nas łączy."),
            def("profil_4", "p4_q3",
                    "Jeśli się wzbogacę, stracę rodzinę lub przyjaciół.",
                    "Wzbogacam się i utrzymuję bliskie relacje."),
            def("profil_4", "p4_q4",
                    "Nie mogę mieć lepiej niż moi rodzice.",
                    "Tworzę nowe wzorce finansowe w swojej rodzinie."),
            def("profil_4", "p4_q5",
                    "Czuję, że muszę dźwigać finansowe ciężary mojej rodziny.",
                    "Oddaję to, co nie jest moje, i zajmuję się własnym życiem."),
            def("profil_4", "p4_q6",
                    "Bieda uszlachetnia.",
                    "Jestem szlachetna i bogata jednocześnie."),
            def("profil_4", "p4_q7",
                    "Boję się, że kiedy mam więcej, ludzie kochają mnie nie za to, kim jestem, tylko za to, co mam.",
                    "Jestem kochana za to, kim jestem, niezależnie od tego, ile mam."),
            def("profil_4", "p4_q8",
                    "W mojej rodzinie wszyscy odnieśli sukces finansowy, ja też muszę.",
                    "Podążam własną drogą i mam prawo definiować sukces po swojemu."),
            def("profil_4", "p4_q9",
                    "Nie mogę mieć tego, czego moja mama/babcia nie miała.",
                    "Tworzę nowe, lepsze wzorce finansowe dla swojego pokolenia.")
    );

    // ── PROFIL 5 — Wycofana Liderka (pytania 37–45) ─────────────────────────

    public static final List<StatementDefinition> PROFIL_5 = List.of(
            def("profil_5", "p5_q1",
                    "Ludzie mogą mnie skrzywdzić, gdy zobaczą, że mam pieniądze.",
                    "Bycie bogatą jest bezpieczne."),
            def("profil_5", "p5_q2",
                    "Mój sukces finansowy wywołuje zazdrość innych.",
                    "Sukces finansowy może budzić inspirację i szacunek."),
            def("profil_5", "p5_q3",
                    "Kiedy okazuję zaufanie, ludzie to wykorzystują.",
                    "Mogę ufać i zachować swoje granice."),
            def("profil_5", "p5_q4",
                    "Im więcej mam, tym bardziej jestem narażona na ataki.",
                    "Mogę mieć dużo i czuć się chroniona."),
            def("profil_5", "p5_q5",
                    "Sukces finansowy to samotność.",
                    "Mogę cieszyć się sukcesem finansowym, otoczona ludźmi, których lubię."),
            def("profil_5", "p5_q6",
                    "Pokazanie, że mam więcej, sprowokuje ataki.",
                    "Mogę mieć więcej i czuć się bezpieczna."),
            def("profil_5", "p5_q7",
                    "Jeśli będę bogata, ludzie, których znam, się ode mnie odsuną.",
                    "Mogę mieć więcej i nadal być blisko z ludźmi, którzy są dla mnie ważni."),
            def("profil_5", "p5_q8",
                    "Lepiej się nie wychylać, wtedy będę bezpieczna.",
                    "Mogę być widoczna i nadal czuć się bezpiecznie."),
            def("profil_5", "p5_q9",
                    "Sukces odbierze mi prywatność.",
                    "Mogę mieć sukces i zachować prywatność, której potrzebuję.")
    );

    // ── PROFIL 6 — Zapracowana Perfekcjonistka (pytania 46–54) ──────────────

    public static final List<StatementDefinition> PROFIL_6 = List.of(
            def("profil_6", "p6_q1",
                    "Na pieniądze trzeba ciężko pracować i się poświęcać.",
                    "Zarabiam pieniądze w lekki i przyjemny sposób."),
            def("profil_6", "p6_q2",
                    "Nie mogę podnieść cen, bo stracę klientów.",
                    "Podnoszę ceny i nadal mam klientów."),
            def("profil_6", "p6_q3",
                    "Zawsze muszę być produktywna, czuję się winna, gdy nie robię wystarczająco.",
                    "Potrafię odpoczywać bez poczucia winy."),
            def("profil_6", "p6_q4",
                    "Wypalenie jest ceną sukcesu.",
                    "Sukces dodaje energii."),
            def("profil_6", "p6_q5",
                    "Zarabianie to stres.",
                    "Zarabianie jest przyjemne i daje mi satysfakcję."),
            def("profil_6", "p6_q6",
                    "Trzeba mieć „fach w ręku“, żeby zarabiać.",
                    "Zarabiam dzięki swoim talentom i pomysłom."),
            def("profil_6", "p6_q7",
                    "Muszę wyglądać, jakbym wszystko miała pod kontrolą.",
                    "Nie muszę być idealna, żeby zasługiwać na szacunek."),
            def("profil_6", "p6_q8",
                    "Na przyjemność trzeba zasłużyć.",
                    "Cieszę się przyjemnością bez poczucia winy."),
            def("profil_6", "p6_q9",
                    "Jeśli ja tego nie udźwignę, wszystko się zawali.",
                    "Świat może działać dobrze, nawet gdy ja odpoczywam.")
    );

    // ── PROFIL 7 — Zatrzymana w Przyjmowaniu (pytania 55–63) ────────────────

    public static final List<StatementDefinition> PROFIL_7 = List.of(
            def("profil_7", "p7_q1",
                    "Moja praca jest warta mniej niż dostaję.",
                    "Moja praca jest warta co najmniej tyle, ile dostaję."),
            def("profil_7", "p7_q2",
                    "Nie powinno się brać pieniędzy za pracę z energią/duchowością.",
                    "Pomagam innym na poziomie energetycznym i duchowym i dobrze zarabiam."),
            def("profil_7", "p7_q3",
                    "Trudno mi przyjąć coś bez poczucia, że powinnam się odwdzięczyć.",
                    "Przyjmuję z wdzięcznością, bez obowiązku rewanżu."),
            def("profil_7", "p7_q4",
                    "Nie wypada przyjmować pieniędzy, jeśli na nie nie zapracowałam.",
                    "Przyjmuję pieniądze z wdzięcznością, nawet gdy nie były wynikiem mojego wysiłku."),
            def("profil_7", "p7_q5",
                    "Przyjmowanie prezentów lub pieniędzy jest krępujące.",
                    "Przyjmowanie prezentów i pieniędzy jest powodem do wdzięczności i radości."),
            def("profil_7", "p7_q6",
                    "Ludzie lubią mnie tylko, gdy im coś daję.",
                    "Jestem lubiana i kochana za to, kim jestem."),
            def("profil_7", "p7_q7",
                    "Nie wypada brać pieniędzy za coś, co przychodzi mi z łatwością.",
                    "Zarabianie na tym, co przychodzi z lekkością i radością, jest dobre."),
            def("profil_7", "p7_q8",
                    "W moim wieku lepiej zainwestować w coś konkretnego albo w dzieci, niż w siebie.",
                    "Zawsze warto inwestować w siebie, niezależnie od wieku i etapu życia."),
            def("profil_7", "p7_q9",
                    "Pozwalanie sobie na luksus to przesada.",
                    "Potrafię cieszyć się luksusem bez poczucia winy.")
    );

    // ── PROFIL 8 — Idealistka Skromności (pytania 64–72) ────────────────────

    public static final List<StatementDefinition> PROFIL_8 = List.of(
            def("profil_8", "p8_q1",
                    "Bieda jest cnotą.",
                    "Posiadanie pieniędzy nie wyklucza bycia dobrym człowiekiem."),
            def("profil_8", "p8_q2",
                    "Cieszenie się pieniędzmi świadczy o braku pokory.",
                    "Cieszenie się pieniędzmi i bycie pokorną nie wykluczają się."),
            def("profil_8", "p8_q3",
                    "Pragnienie bogactwa jest złe.",
                    "Pragnę bogactwa mając czyste intencje."),
            def("profil_8", "p8_q4",
                    "Kiedy ludzie mają za dużo, to się zmieniają na gorsze.",
                    "Posiadanie dużych pieniędzy nie oznacza, że człowiek zmieni się na gorsze."),
            def("profil_8", "p8_q5",
                    "Tylko skromne życie jest wartościowe.",
                    "Wartość życia nie zależy od tego, ile mam."),
            def("profil_8", "p8_q6",
                    "Bogactwo często psuje ludzi.",
                    "Bogactwo może pokazywać to, co w człowieku dobre."),
            def("profil_8", "p8_q7",
                    "Nie wypada mieć więcej niż inni.",
                    "Mam więcej niż inni i nadal jestem dobrym człowiekiem."),
            def("profil_8", "p8_q8",
                    "Pieniądze oddzielają ludzi od siebie.",
                    "Pieniądze mogą wspierać relacje i bliskość."),
            def("profil_8", "p8_q9",
                    "Posiadanie dużych pieniędzy oddziela od duchowości.",
                    "Pieniądze mogą wspierać mój rozwój duchowy.")
    );

    public static final List<StatementDefinition> ALL = Stream.of(
            PROFIL_1, PROFIL_2, PROFIL_3, PROFIL_4,
            PROFIL_5, PROFIL_6, PROFIL_7, PROFIL_8
    ).flatMap(List::stream).toList();

    // Seeded definitions keep deterministic p{n}_q{n} keys (reproducible across environments);
    // user-created ones get server-generated "sk_" + UUID keys. Both are opaque references.
    private static StatementDefinition def(String profileId, String statementKey,
                                           String limiting, String supporting) {
        return StatementDefinition.builder()
                .profileId(profileId)
                .statementKey(statementKey)
                .statementTypeDefinitions(List.of(
                        new StatementTypeDefinition(StatementType.LIMITING, limiting),
                        new StatementTypeDefinition(StatementType.SUPPORTING, supporting)
                ))
                .build();
    }
}
