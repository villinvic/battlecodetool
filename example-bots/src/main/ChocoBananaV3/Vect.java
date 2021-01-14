package ChocoBananaV3;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Vect {

    public double x;
    public double y;
    public double totalWeight=0;


    public Vect(){
        this.x = 0;
        this.y = 0;
    }

    public void update(MapLocation target, MapLocation self, int weight){
        double dx, dy, norm;
        dx = (target.x - self.x);
        dy = (target.y - self.y);
        norm = Math.abs(dx)+ Math.abs(dy);
        this.x += weight * dx/norm;
        this.y += weight * dy/norm;
        this.totalWeight += Math.abs(weight);
    }

    public void update(Direction dir, int weight){
        double dx, dy, norm;
        dx = dir.getDeltaX();
        dy = dir.getDeltaY();
        norm = Math.abs(dx)+ Math.abs(dy);
        this.x += weight * dx/norm;
        this.y += weight * dy/norm;
        this.totalWeight += Math.abs(weight);
    }

    public void reset(){
        this.x = 0;
        this.y = 0;
        this.totalWeight = 0;
    }

    public double distance(MapLocation target, MapLocation self){
        double dx, dy;
        dx = (target.x - self.x);
        dy = (target.y - self.y);
        return Math.abs(dx)+ Math.abs(dy);
    }

    public boolean isnull(){
        return (Math.abs(this.x)+ Math.abs(this.y) ) == 0;
    }
}
