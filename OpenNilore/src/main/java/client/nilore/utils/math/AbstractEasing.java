package client.nilore.utils.math;

import client.nilore.utils.math.Easing;
import client.nilore.utils.math.Point2d;

public abstract class AbstractEasing
implements Easing {
    public final Point2d p1 = new Point2d(0.0, 0.0);
    public final Point2d p2 = new Point2d(1.0, 1.0);
}