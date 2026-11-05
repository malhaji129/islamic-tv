package org.local.karbalascreen;
import java.util.*;
public final class ContentLayoutTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        String text = "إِنَّ ٱللَّهَ عَلَىٰ كُلِّ شَىْءٍۢ قَدِيرٌۭ ".repeat(30);
        List<String> pages = ContentPagination.split(text, 180);
        check(pages.size() > 1, "Long Arabic text must paginate");
        check(String.join("", pages).equals(text), "Pagination must preserve every source character");
        for (int i=0; i<pages.size(); i++) {
            check(pages.get(i).length() <= 180, "Page exceeds the TV character budget");
            if (i<pages.size()-1) check(Character.isWhitespace(pages.get(i).charAt(pages.get(i).length()-1)), "Split must use word boundary");
        }
        check(ContentPagination.split("آية قصيرة",180).equals(Arrays.asList("آية قصيرة")), "Short text unchanged");
        check(ContentPagination.split("",180).isEmpty(), "Empty text has no page");
        String word = "ك".repeat(200);
        check(ContentPagination.split(word,180).equals(Arrays.asList(word)), "Do not corrupt a long word");
        List<String> ordered = ContentSequence.balance(Arrays.asList(Arrays.asList("Q1","Q2","Q3"), Arrays.asList("H1","H2"), Arrays.asList("D1")));
        check(ordered.equals(Arrays.asList("Q1","H1","D1","Q2","H2","D1","Q3","H1","D1")), "Every category receives one turn and all items are reachable");
        check(ContentSequence.balance(Arrays.asList(Collections.<String>emptyList(),Arrays.asList("Q1"))).equals(Arrays.asList("Q1")), "Empty categories skipped");
        System.out.println("Content pagination and balanced rotation tests passed");
    }
}
