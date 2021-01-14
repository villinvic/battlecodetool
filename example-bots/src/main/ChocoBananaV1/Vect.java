package  ChocoBananaV1;

import battlecode.common.MapLocation;

public class Vect {

    public double x;
    public double y;


    public Vect(){
        this.x = 0;
        this.y = 0;
    }

    public void update(MapLocation target, MapLocation self, int weight){
        double dx, dy, norm;
        dx = (target.x - self.x);
        dy = (target.y - self.y);
        norm = Math.sqrt(dx*dx +dy*dy);
        this.x += weight * (target.x - self.x)/norm;
        this.y += weight * (target.y - self.y)/norm;
    }

    public void reset(){
        this.x = 0;
        this.y = 0;
    }

    public boolean isnull(){
        return (Math.abs(this.x)+ Math.abs(this.y) ) == 0;
    }
}
