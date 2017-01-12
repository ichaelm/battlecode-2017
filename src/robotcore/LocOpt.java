package robotcore;

import battlecode.common.*;

public class LocOpt {

    /*
    private class Item {
        public Item() {
        }
    }

    private class CircleItem extends Item {
        MapLocation center;
        float radius;
        CircleItem(MapLocation center, float radius) {
            this.center = center;
            this.radius = radius;
        }
    }

    private class LineSegmentItem extends Item {
        MapLocation pointA;
        MapLocation pointB;
        LineSegmentItem(MapLocation pointA, MapLocation pointB) {
            this.pointA = pointA;
            this.pointB = pointB;
        }
    }
    */

    /*
    private class BoundaryItem extends Item{
        private float xFactor;
        private float yFactor;
        private float threshold; // greater than threshold represents intersection
        public BoundaryItem(int priority, MapLocation origin, Direction dir) {
            super(priority);
            float dx = dir.getDeltaX(1);
            float dy = dir.getDeltaY(1);
        }
    }
    */

    private enum BoundaryType {LINE, ARC}

    private class Boundary {
        MapLocation begin;
        Boundary next;
        BoundaryType type;
        Boundary(MapLocation begin, BoundaryType type) {
            this.begin = begin;
            this.next = null;
            this.type = type;
        }
    }

    private class LineBoundary extends Boundary {
        LineBoundary(MapLocation begin) {
            super(begin, BoundaryType.LINE);
        }
    }

    private class ArcBoundary extends Boundary {
        MapLocation center;
        boolean clockwise;
        ArcBoundary(MapLocation begin, MapLocation center, boolean clockwise) {
            super(begin, BoundaryType.ARC);
            this.center = center;
            this.clockwise = clockwise;
        }
    }

    private float bodyRadius;
    private float strideRadius;

    private Boundary first;

    public LocOpt(RobotType robotType, MapLocation loc) {
        this.bodyRadius = robotType.bodyRadius;
        this.strideRadius = robotType.strideRadius;
        first = new ArcBoundary(loc.add(Direction.getSouth(), strideRadius), loc, true);
        first.next = first;
    }

    /*
    public void addIncludeCircle(MapLocation center, float radius) {
        Item item = new CircleItem(center, radius);
        includeItems[numIncludeItems] = item;
        numIncludeItems++;
    }
    */

    private MapLocation[] getCircleIntersections(MapLocation centerA, float radiusA, MapLocation centerB, float radiusB) {
        // calculate distance
        float dist = centerA.distanceTo(centerB);
        Direction dir = centerA.directionTo(centerB);
        // calculate location of intersections perpendicular to intersection line
        float perpIntermediate = (dist + radiusA + radiusB) * (-dist + radiusA + radiusB) * (-dist + radiusA - radiusB) * (-dist - radiusA + radiusB);
        if (perpIntermediate < 0) {
            return new MapLocation[]{};
        } else {
            // calculate location of intersections parallel to intersection line
            float parallel = ((dist * dist) + (radiusA * radiusA) - (radiusB * radiusB)) / (2 * dist);
            MapLocation intersectionCenter = centerA.add(dir, parallel);
            if (perpIntermediate == 0) {
                return new MapLocation[]{intersectionCenter};
            } else {
                float perp = (float)Math.sqrt(perpIntermediate) / (dist * 2);
                MapLocation intersectionLeft = intersectionCenter.add(dir.rotateLeftDegrees(90), perp);
                MapLocation intersectionRight = intersectionCenter.add(dir.rotateRightDegrees(90), perp);
                return new MapLocation[]{intersectionLeft, intersectionRight};
            }
        }
    }

    private MapLocation[] selectArcIntersections(ArcBoundary bound, MapLocation[] intersections) {
        Direction initialDir = bound.center.directionTo(bound.begin);
        Direction finalDir = bound.center.directionTo(bound.next.begin);
        float maxOrdinal = finalDir.radians - initialDir.radians;
        if (maxOrdinal < 0) {
            maxOrdinal += 2*Math.PI;
        }
        if (bound.clockwise) {
            maxOrdinal = 2*(float)Math.PI - maxOrdinal;
        }
        if (maxOrdinal == 0) {
            maxOrdinal += 2*Math.PI;
        }

        MapLocation[] selected = new MapLocation[intersections.length];
        float[] ordinals = new float[intersections.length];
        int numSelected = 0;
        for (MapLocation intersection : intersections) {
            Direction intersectionDir = bound.center.directionTo(intersection);
            float ordinal = intersectionDir.radians - initialDir.radians;
            if (ordinal < 0) {
                ordinal += 2*Math.PI;
            }
            if (bound.clockwise) {
                ordinal = 2*(float)Math.PI - ordinal;
            }
            if (ordinal < maxOrdinal) {
                // insert
                int i = 0;
                while (i < numSelected && ordinals[i] > ordinal) { // Short circuiting is important
                    i++;
                }
                MapLocation replace = selected[i];
                float replaceOrdinal = ordinals[i];
                selected[i] = intersection;
                ordinals[i] = ordinal;
                numSelected++;
                while (i + 1 < numSelected) {
                    i++;
                    // swap
                    MapLocation loc = selected[i];
                    float ord = ordinals[i];
                    selected[i] = replace;
                    ordinals[i] = replaceOrdinal;
                    replace = loc;
                    replaceOrdinal = ord;
                }
            }
        }
        MapLocation[] trimmedSelected;
        if (numSelected == intersections.length) {
            trimmedSelected = selected;
        } else {
            trimmedSelected = new MapLocation[numSelected];
            for (int i = 0; i < numSelected; i++) {
                trimmedSelected[i] = selected[i];
            }
        }
        return trimmedSelected;
    }

    private MapLocation getArcSegmentMiddle(ArcBoundary bound, MapLocation segmentBegin, MapLocation segmentEnd) {
        float radiansBegin = bound.center.directionTo(segmentBegin).radians;
        float radiansEnd = bound.center.directionTo(segmentEnd).radians;
        if (bound.clockwise) {
            while (radiansEnd > radiansBegin) {
                radiansEnd -= 2*(float)Math.PI;
            }
        } else {
            while (radiansEnd < radiansBegin) {
                radiansEnd += 2*(float)Math.PI;
            }
        }
        float radiansMiddle = (radiansBegin + radiansEnd) / 2;
        if (radiansMiddle < 0) {
            radiansMiddle += 2*(float)Math.PI;
        }
        if (radiansMiddle > 2*(float)Math.PI) {
            radiansMiddle -= 2*(float)Math.PI;
        }
        return bound.center.add(new Direction(radiansMiddle), bound.center.distanceTo(bound.begin));
    }

    public void addExcludeCircle(MapLocation center, float radius) {
        // If everything is excluded, skip
        if (first == null) {
            return;
        }



        // If the first point is excluded, rotate until first point is not excluded
        Boundary current = first;
        Boundary prev = null;
        boolean firstIter = true;
        while (current != null) {
            // On first iter, test first point is valid
            if (firstIter) {
                if (current.begin.distanceTo(center) <= radius + bodyRadius) {
                    // first point is excluded
                    // TODO
                }
            }
            // Now, first point is valid

            switch (current.type) {
                case ARC: {
                    ArcBoundary currentArc = (ArcBoundary)current;
                    // get intersection points
                    MapLocation[] intersections = getCircleIntersections(currentArc.begin, strideRadius,
                                                                         center, bodyRadius + radius);
                    // Select intersection points on the current arc, in order
                    MapLocation[] selectedIntersections = selectArcIntersections(currentArc, intersections);
                    // Test each segment
                    MapLocation segmentBegin = currentArc.begin;
                    for (MapLocation segmentEnd : selectedIntersections) {
                        MapLocation segmentMiddle = getArcSegmentMiddle(currentArc, segmentBegin, segmentEnd);
                        if (segmentMiddle.distanceTo(center) <= radius + bodyRadius) {
                            // this segment intersects
                        }

                        segmentBegin = segmentEnd;
                    }

                }
                break;
                case LINE: {

                }
                break;
            }

            prev = current;
            current = current.next;
            firstIter = false;
        }
    }

    /*
    public void addIncludeLineSegment(MapLocation pointA, MapLocation pointB) {
        Item item = new LineSegmentItem(pointA, pointB);
        includeItems[numIncludeItems] = item;
        numIncludeItems++;
    }
    */

    public void addExcludeLineSegmentItem(MapLocation pointA, MapLocation pointB) {
        //Item item = new LineSegmentItem(pointA, pointB);
        //excludeItems[numExcludeItems] = item;
        //numExcludeItems++;
    }

    public MapLocation optimizeTowardsTarget(MapLocation target) {
        return null;
    }

    public MapLocation optimizeAwayFromTarget(MapLocation target) {
        return null;
    }
}
