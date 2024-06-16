package RTExtended.HitTable;

import RTExtended.HitInfo.HitRecord;
import RTExtended.HitInfo.Interval;
import RTExtended.Material.Isotropic;
import RTExtended.Ray;
import RTExtended.Color;
import RTExtended.Vec3;

import static RTExtended.RTUtil.random_double;
import static RTExtended.RTUtil.infinity;


public class Volume implements HitTable {
    HitTable boundary;
    double neg_inv_density;
    Color sigma_a, sigma_s, sigma_t;
    record volumeHitInfo(double first_p, double distance, Vec3 tr){}

    public Volume(HitTable b, double d, Color a, Color s){
        boundary = b;
        neg_inv_density = -1/d;
        sigma_a = a;
        sigma_s = s;
        sigma_t = sigma_a.add(sigma_s);
    }
    

    @Override
    public boolean hit(Ray r, Interval t, HitRecord rec) {
        volumeHitInfo volume_info = getVolumeHitInfo(r, t, boundary);

        double distance_inside_boundary = volume_info.distance * r.dir().length();
        double hit_distance = neg_inv_density * Math.log(random_double());

        if (hit_distance >= distance_inside_boundary)
                return false;

        rec.t = volume_info.first_p + hit_distance / r.dir().length();
        rec.p = r.at(rec.t);
        rec.normal = new Vec3(0);
        rec.front_face = true;
        rec.mat = new Isotropic(sigma_a.add(sigma_s.multiply(volume_info.tr)));

        return true;
    }

    volumeHitInfo getVolumeHitInfo(Ray r, Interval t, HitTable boundary) {
        boolean is_first = true;
        double first_point = 0;
        double distance = 0;
        Vec3 tr = new Vec3(1);

        HitRecord hit_p1 = new HitRecord(), hit_p2 = new HitRecord();
        Interval nxte_t = new Interval(-infinity, infinity);
        while (boundary.hit(r, nxte_t, hit_p1)) {
            if (hit_p1.t < t.min()) hit_p1.t = t.min();
            if (hit_p1.t < 0)       hit_p1.t = 0;
            if (is_first) {
                first_point = hit_p1.t;
                is_first = false;
            }

            nxte_t = new Interval(hit_p1.t + 1e-5, infinity);
            if (boundary.hit(r, nxte_t, hit_p2)) {
                if (hit_p2.t > t.max())     hit_p2.t = t.max();
                if (hit_p1.t >= hit_p2.t)   break;

                distance += (hit_p2.t - hit_p1.t);
                tr.multiply_Equal(Vec3.Exp(sigma_t.negative().multiply(distance)));
                nxte_t = new Interval(hit_p2.t + 1e-5, infinity);
            }
            else break;
        }
        return new volumeHitInfo(first_point, distance, tr);
    }

    @Override
    public AABB bounding_box() {
        return boundary.bounding_box();
    }
}
