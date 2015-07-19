package lexek.wschat.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Colors {
    private static final Pattern pattern = Pattern.compile("#[a-zA-Z0-9]{6}");
    private static final Map<String, String> colors = ImmutableMap
        .<String, String>builder()
        .put("red", "#FF0000")
        .put("blue", "#005dff")
        .put("green", "#008000")
        .put("firebrick", "#B22222")
        .put("coral", "#FF7F50")
        .put("yellowgreen", "#9ACD32")
        .put("orangered", "#FF4500")
        .put("seagreen", "#2E8B57")
        .put("goldenrod", "#DAA520")
        .put("chocolate", "#D2691E")
        .put("cadetblue", "#5F9EA0")
        .put("dodgerblue", "#1E90FF")
        .put("hotpink", "#FF69B4")
        .put("blueviolet", "#8A2BE2")
        .put("springgreen", "#00BA5D")
        .put("gray", "#808080")
        .put("deeppink", "#FF1493")
        .build();
    private static final List<String> colorList = ImmutableList.copyOf(colors.values());

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