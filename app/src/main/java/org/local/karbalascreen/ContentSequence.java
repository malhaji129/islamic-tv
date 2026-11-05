package org.local.karbalascreen;

import java.util.ArrayList;
import java.util.List;

/** Give each nonempty category one turn, reaching every source record in a cycle. */
final class ContentSequence {
    private ContentSequence() {}
    static <T> List<T> balance(List<List<T>> categories) {
        List<T> sequence = new ArrayList<>();
        int longest = 0;
        for (List<T> category : categories) longest = Math.max(longest, category.size());
        for (int turn = 0; turn < longest; turn++) {
            for (List<T> category : categories) {
                if (!category.isEmpty()) sequence.add(category.get(turn % category.size()));
            }
        }
        return sequence;
    }
}
