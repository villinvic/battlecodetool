package ChocoBananaV4;

public final class Flags {
    public static final int DANGER = 1 << 0; // Also used for following units going toward ECs
    public static final int NEARSLAND = 1 << 2;
    public static final int NEARPOL = 1 << 3;
    public static final int NEARCENTER = 1 << 4;
    public static final int NEARMUCK = 1 << 5;

    public static final int NOT_SCOUTABLE = 1 << 6;
    public static final int FAR = 1 << 7;
    public static final int MEDIUM = 1 << 1;
    public static final int FOUND_NEUTRAL_EC = 1 << 8;
    public static final int NEARCENTER_2 = 1 << 9;

    public static final int NEARSELFPOL = 1 << 10;
    public static final int NEARSELFSLAND = 1 << 11;
    public static final int NEARSELFCENTER = 1 << 12;
    public static final int NEARSELFMUCK = 1 << 13;

    public static final int OBSTRUCTED = 1 << 14;
    public static final int FOUND = 1 << 15;

    public static final int LEFT = 1 << 16;
    public static final int UPLEFT = 1 << 17;
    public static final int UP = 1 << 18;
    public static final int UPRIGHT = 1 << 19;
    public static final int RIGHT = 1 << 20;
    public static final int DOWNRIGHT = 1 << 21;
    public static final int DOWN = 1 << 22;
    public static final int DOWNLEFT = 1 << 23;

}