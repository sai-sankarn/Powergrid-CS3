package Frontend;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static class CityCoordinates {
        public static final Map<String, Point> coordinates = new HashMap<>();

        static {
            // North (Teal)
            coordinates.put("Flensburg", new Point(354, 27));
            coordinates.put("Kiel", new Point(402, 88));
            coordinates.put("Cuxhaven", new Point(291, 143));
            coordinates.put("Wilhelmshaven", new Point(228, 180)); // Fixed typo
            coordinates.put("Hamburg", new Point(397, 180));
            coordinates.put("Bremen", new Point(307, 236));
            coordinates.put("Hannover", new Point(399, 325));

            // Northeast (Brown)
            coordinates.put("Lubeck", new Point(471, 128));
            coordinates.put("Rostock", new Point(587, 105)); // Fixed typo
            coordinates.put("Schwerin", new Point(532, 184));
            coordinates.put("Torgelow", new Point(762, 177)); // Fixed typo
            coordinates.put("Magdeburg", new Point(566, 333)); // Fixed typo
            coordinates.put("Berlin", new Point(702, 303));
            coordinates.put("Frankfurt-O", new Point(797, 324)); // Matched "Frankfurt-O"

            // West (Red)
            coordinates.put("Osnabrück", new Point(237, 311)); // Added umlaut
            coordinates.put("Münster", new Point(186, 367)); // Added umlaut
            coordinates.put("Duisburg", new Point(62, 392));
            coordinates.put("Essen", new Point(120, 410));
            coordinates.put("Dortmund", new Point(202, 436)); // Fixed typo
            coordinates.put("Düsseldorf", new Point(77, 462)); // Added umlaut
            coordinates.put("Kassel", new Point(354, 450));

            // East/Central (Yellow)
            coordinates.put("Halle", new Point(581, 424));
            coordinates.put("Leipzig", new Point(631, 449));
            coordinates.put("Dresden", new Point(761, 488));
            coordinates.put("Erfurt", new Point(515, 485));
            coordinates.put("Fulda", new Point(398, 537));
            coordinates.put("Würzburg", new Point(413, 627)); // Added umlaut
            coordinates.put("Nürnberg", new Point(516, 667)); // Fixed typo & added umlaut

            // Southwest (Blue)
            coordinates.put("Aachen", new Point(49, 527));
            coordinates.put("Köln", new Point(139, 506)); // Added umlaut
            coordinates.put("Trier", new Point(87, 634));
            coordinates.put("Wiesbaden", new Point(241, 597));
            coordinates.put("Frankfurt-M", new Point(298, 576)); // Matched "Frankfurt-M"
            coordinates.put("Saarbrücken", new Point(167, 701)); // Added umlaut
            coordinates.put("Mannheim", new Point(288, 682));

            // South (Purple)
            coordinates.put("Stuttgart", new Point(317, 757));
            coordinates.put("Freiburg", new Point(208, 832));
            coordinates.put("Konstanz", new Point(316, 871));
            coordinates.put("Augsburg", new Point(469, 781));
            coordinates.put("Regensburg", new Point(589, 723));
            coordinates.put("München", new Point(561, 830)); // Added umlaut
            coordinates.put("Passau", new Point(728, 773));
        }
    }
}