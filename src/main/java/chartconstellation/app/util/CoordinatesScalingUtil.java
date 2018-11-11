package chartconstellation.app.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import chartconstellation.app.entities.response.IdCoordinates;
import chartconstellation.app.entities.response.Point;

@Component
public class CoordinatesScalingUtil {

    @Autowired
    ScalingUtil scalingUtil;

    private List<IdCoordinates> coordinatesList = new ArrayList<>();

    public List<IdCoordinates> getScaledCoordinates(Double xMin, Double xMax, Double yMin, Double yMax) {

        List<Double> xvalues = new ArrayList<>();
        List<Double> yvalues = new ArrayList<>();

        for(IdCoordinates idCoordinate : coordinatesList) {
            xvalues.add(idCoordinate.getPoint().getX());
            yvalues.add(idCoordinate.getPoint().getY());
        }

        scalingUtil.initialize(xvalues, yvalues);

        for(int i=0; i<coordinatesList.size(); i++) {
            Point point = coordinatesList.get(i).getPoint();
            Double scaledXvalue = scalingUtil.getScaledvalueX(point.getX(), xMin, xMax);
            Double scaledYvalue = scalingUtil.getScaledvalueY(point.getY(), yMin, yMax);
            point.setX(scaledXvalue);
            point.setY(scaledYvalue);
            coordinatesList.get(i).setPoint(point);
        }

        return coordinatesList;
    }

    public List<IdCoordinates> getCoordinatesList() {
        return coordinatesList;
    }

    public void setCoordinatesList(List<IdCoordinates> coordinatesList) {
        this.coordinatesList = coordinatesList;
    }
}
