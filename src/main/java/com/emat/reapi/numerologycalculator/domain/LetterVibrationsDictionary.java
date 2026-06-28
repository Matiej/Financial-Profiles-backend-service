package com.emat.reapi.ncalculator.domain;

import java.util.Map;
import java.util.Set;

public class LetterVibrationsDictionary {
    public static final Map<Character, Integer> LETTER_VIBRATION = Map.ofEntries(
            // 1
            Map.entry('A', 1),
            Map.entry('Ą', 1),
            Map.entry('J', 1),
            Map.entry('S', 1),
            Map.entry('Ś', 1),

            // 2
            Map.entry('B', 2),
            Map.entry('K', 2),
            Map.entry('T', 2),

            // 3
            Map.entry('C', 3),
            Map.entry('Ć', 3),
            Map.entry('L', 3),
            Map.entry('Ł', 3),
            Map.entry('U', 3),

            // 4
            Map.entry('D', 4),
            Map.entry('M', 4),
            Map.entry('V', 4),

            // 5
            Map.entry('E', 5),
            Map.entry('Ę', 5),
            Map.entry('N', 5),
            Map.entry('Ń', 5),
            Map.entry('W', 5),

            // 6
            Map.entry('F', 6),
            Map.entry('O', 6),
            Map.entry('Ó', 6),
            Map.entry('X', 6),

            // 7
            Map.entry('G', 7),
            Map.entry('P', 7),
            Map.entry('Y', 7),

            // 8
            Map.entry('H', 8),
            Map.entry('Q', 8),
            Map.entry('Z', 8),
            Map.entry('Ż', 8),
            Map.entry('Ź', 8),

            // 9
            Map.entry('I', 9),
            Map.entry('R', 9)
    );

    public static final Set<Character> VOWELS = Set.of(
            'A', 'Ą', 'E', 'Ę', 'I', 'O', 'Ó', 'U', 'Y'
    );
}
