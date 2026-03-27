package Frontend;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static class CityCoordinates{
        public static final Map<String, Point> coordinates = new HashMap<>();

        static {
            coordinates.put("Flensburg", new Point(354,27));
            coordinates.put("Kiel", new Point(402,88));
            coordinates.put("Lubeck", new Point(471,128));
            coordinates.put("Hostock", new Point(587,105));
            coordinates.put("Schwerin", new Point(532,184));
            coordinates.put("Torcelow", new Point(762,177));
            coordinates.put("Hamburg", new Point(397,180));
            coordinates.put("Cuxhaven", new Point(291,143));
            coordinates.put("Wilmhelmshaven", new Point(228,180));
            coordinates.put("Bremen", new Point(307,236));
            coordinates.put("Osnabruck", new Point(237,311));
            coordinates.put("Hannover", new Point(339,325));
            coordinates.put("Magdeburg", new Point(556,333));
            coordinates.put("Berlin", new Point(702,303));
            coordinates.put("Frankfurt", new Point(797,324));
            coordinates.put("Munster", new Point(186,367));
            coordinates.put("Duisburg", new Point(62,392));
            coordinates.put("Essen", new Point(120,410));
            coordinates.put("Dartmund", new Point(202,436));
            coordinates.put("Kassel", new Point(354,450));
            coordinates.put("Dusseldorf", new Point(77,462));
            coordinates.put("Halle", new Point(581,424));
            coordinates.put("Leipzig", new Point(631,449));
            coordinates.put("Dresden", new Point(761,488));
        }

    }
}
