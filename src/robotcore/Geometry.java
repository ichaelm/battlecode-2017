package robotcore;

import battlecode.common.*;

public class Geometry {

    public static MapLocation[] getCircleIntersections(MapLocation centerA, float radiusA, MapLocation centerB, float radiusB) {
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
            float perp = (float)Math.sqrt(perpIntermediate) / (dist * 2);
            MapLocation intersectionLeft = intersectionCenter.add(dir.rotateLeftDegrees(90), perp);
            MapLocation intersectionRight = intersectionCenter.add(dir.rotateRightDegrees(90), perp);
            //float radiansLeft = centerA.directionTo(intersectionLeft).radians;
            //float radiansRight = centerA.directionTo(intersectionRight).radians;
            return new MapLocation[]{intersectionLeft, intersectionRight};
        }
    }

    public static MapLocation[] getCircleLineSegmentIntersections(MapLocation center, float r, MapLocation lineA, MapLocation lineB) {
        float dx = lineB.x - lineA.x;
        float dy = lineB.y - lineA.y;
        float fx = lineA.x - center.x;
        float fy = lineA.y - center.y;
        float a = dx * dx + dy * dy;
        float b = 2 * (dx * fx + dy * fy);
        float c = fx * fx + fy * fy - r * r;
        float discriminant = b*b-4*a*c;
        if (discriminant < 0) {
            // no intersection
            return new MapLocation[]{};
        } else {
            // ray didn't totally miss sphere,
            // so there is a solution to
            // the equation.

            float intermediate = (float)Math.sqrt(discriminant);

            // either solution may be on or off the ray so need to test both
            // t1 is always the smaller value, because BOTH discriminant and
            // a are nonnegative.
            float t1 = (-b - intermediate)/(2*a);
            float t2 = (-b + intermediate)/(2*a);
            MapLocation intersection1 = null;
            MapLocation intersection2 = null;

            if (t1 >= 0 && t1 <= 1) {
                intersection1 = lineA.translate(t1 * dx, t1 * dy);
            }

            if (t2 >= 0 && t2 <= 1) {
                intersection2 = lineA.translate(t2 * dx, t2 * dy);
            }

            // return stuff
            if (intersection1 == null) {
                if (intersection2 == null) {
                    return new MapLocation[]{};
                } else {
                    return new MapLocation[]{intersection2};
                }
            } else {
                if (intersection2 == null) {
                    return new MapLocation[]{intersection1};
                } else {
                    return new MapLocation[]{intersection1, intersection2};
                }
            }

        }
    }
    public static float[][] getCirclePolygonIntersectionPairs(MapLocation center, float r, MapLocation[] poly) {
    	// start with a point that's outside the circle
    	int start = -1;
    	for (int i = 0; i < poly.length; i++) {
    		if (center.distanceTo(poly[i]) > r) {
    			start = i;
    			break;
    		}
    	}
    	if (start < 0) {
    		// Polygon is completely inside
    		return new float[][]{};
    	}

    	// get intersections from each side
    	MapLocation[] intersections = new MapLocation[10];
    	int numIntersections = 0;
    	for (int i = start; i < start+poly.length; i++) {
    		int iMod = i % poly.length;
    		int jMod = (i+1) % poly.length;
    		MapLocation[] lineIntersections = getCircleLineSegmentIntersections(center, r, poly[iMod], poly[jMod]);
    		for (MapLocation lineIntersection : lineIntersections) {
    			if (numIntersections >= 10) {
    				System.out.println("Too many intersections found!!!");
    			} else {
    				intersections[numIntersections] = lineIntersection;
    				numIntersections++;
    			}
    		}
    	}

    	// Handle less than 2 intersections
    	if (numIntersections == 0) {
    		// No intersections
    		return new float[][]{};
    	} else if (numIntersections % 2 == 1) {
    		System.out.println("Odd number of intersections found!!!");
    		return new float[][]{};
    	}

    	// get angle of each intersection
    	float[][] retval = new float[numIntersections][];
    	for (int i = 0; i < retval.length; i++) {
    		retval[i] = new float[]{
    				center.directionTo(intersections[0]).radians,
    				center.directionTo(intersections[1]).radians
    		};
    	}
    	return retval;
    }

}
