package lexek.wschat.util;

import com.google.common.collect.Lists;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Colors {
    private static final Pattern pattern = Pattern.compile("#[a-zA-Z0-9]{6}");
    private static final HashMap<String, String> colors = new HashMap<>(20);
    private static final ArrayList<String> colorList = Lists.newArrayList(colors.values().iterator());

    static {
        colors.put("red", "#FF0000");
        colors.put("blue", "#005dff");
        colors.put("green", "#008000");
        colors.put("firebrick", "#B22222");
        colors.put("coral", "#FF7F50");
        colors.put("yellowgreen", "#9ACD32");
        colors.put("orangered", "#FF4500");
        colors.put("seagreen", "#2E8B57");
        colors.put("goldenrod", "#DAA520");
        colors.put("chocolate", "#D2691E");
        colors.put("cadetblue", "#5F9EA0");
        colors.put("dodgerblue", "#1E90FF");
        colors.put("hotpink", "#FF69B4");
        colors.put("blueviolet", "#8A2BE2");
        colors.put("springgreen", "#00BA5D");
        colors.put("gray", "#808080");
        colors.put("deeppink", "#FF1493");
    }


    private Colors() {
    }

    public static String getColorCode(String color, boolean check) {
        if (colors.containsKey(color)) {
            return colors.get(color);
        } else if (pattern.matcher(color).matches()) {
            if (check) {
                Color c = Color.decode(color);
                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();
                int brightness = (r * 3 + b + g * 4) >> 3;
                if (brightness < 170 && brightness > 70) {
                    return color;
                } else {
                    return null;
                }
            } else {
                return color;
            }
        } else {
            return null;
        }
    }

    public static String generateColor(String string) {
        return colorList.get(Math.abs(string.hashCode() >> 1) % colorList.size());
    }
}