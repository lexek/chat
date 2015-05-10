package lexek.wschat.proxy.twitch;

import java.util.Arrays;

public class TwitchUser {
    private String nick;
    private String color;
    private boolean mod;
    private boolean staff;
    private boolean admin;
    private boolean subscriber;
    private int[] emoticonSets = new int[0];

    public boolean isSubscriber() {
        return subscriber;
    }

    public void setSubscriber(boolean subscriber) {
        this.subscriber = subscriber;
    }

    public boolean isStaff() {
        return staff;
    }

    public void setStaff(boolean staff) {
        this.staff = staff;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isMod() {
        return mod;
    }

    public String getNick() {
        return nick;
    }

    public void setMod(boolean mod) {
        this.mod = mod;
    }

    public void setEmoticonSets(int[] emoticonSets) {
        this.emoticonSets = emoticonSets;
    }

    public int[] getEmoticonSets() {
        return emoticonSets;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    @Override
    public String toString() {
        return "TwitchUser{" +
                "nick='" + nick + '\'' +
                ", color='" + color + '\'' +
                ", mod=" + mod +
                ", staff=" + staff +
                ", admin=" + admin +
                ", subscriber=" + subscriber +
                ", emoticonSets=" + Arrays.toString(emoticonSets) +
                '}';
    }
}
