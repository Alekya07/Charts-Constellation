package chartconstellation.app.Controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chartconstellation.app.entities.Chart;
import chartconstellation.app.entities.Filter;
import chartconstellation.app.util.DbUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import chartconstellation.app.appconfiguration.Configuration;
import chartconstellation.app.appconfiguration.ScalingConfig;
import chartconstellation.app.clustering.Clustering;
import chartconstellation.app.entities.Filters;
import chartconstellation.app.entities.UserCharts;
import chartconstellation.app.entities.response.IdCoordinates;
import chartconstellation.app.util.ChartsUtil;
import chartconstellation.app.util.CoordinatesScalingUtil;
import chartconstellation.app.util.CoordinatesUtil;


@RestController
@RequestMapping("/coordinates")
public class CoordinatesController {

    @Autowired
    Configuration configuration;

    @Autowired
    CoordinatesUtil coordinatesUtil;

    @Autowired
    Clustering clustering;

    @Autowired
    CoordinatesScalingUtil coordinatesScalingUtil;

    @Autowired
    ChartsUtil chartsUtil;

    @Autowired
    DbUtil dbUtil;

    @RequestMapping(value="/getCoordinates", method= RequestMethod.GET)
    @ResponseBody
    public Collection<List<IdCoordinates>> coordinates(@RequestParam("descWeight") Double descWeight,
                                                       @RequestParam("attrWeight") Double attrWeight,
                                                       @RequestParam("chartEncodingWeight") Double chartEncodingWeight,
                                                       @RequestParam("colorMap") Object colorMap) {


        HashMap<Integer, List<IdCoordinates>> coordinatesMap = getCoordinates(descWeight, attrWeight, chartEncodingWeight, colorMap);
        return coordinatesMap.values();
    }

    @RequestMapping(value="/updateFilter", method= RequestMethod.GET)
    public void filterCoordinates(@RequestParam("filter") String filterMap) {
    	try {

			Filters filters =  new ObjectMapper().readValue(filterMap, Filters.class);
			List<Filter> filtersList = filters.getFilterList();
			List<String> users = null;
			List<String> charts = null;

			for(Filter filter : filtersList) {
                Map<String, List<String>> map = filter.getMap();
                if(map.containsKey("users")) {
                    users = map.get("users");
                }
                if(map.containsKey("charts")){
                    charts = map.get("charts");
                }
                if(map.containsKey("weights")) {

                }
            }

            List<Chart> chartObjs = dbUtil.searchDocsInaCollection(configuration.getMongoDatabase(), configuration.getOlympicchartcollection(), users, charts);
			System.out.println("charts size "+chartObjs.size());

		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public HashMap<String, String> getIdUserMap() {

        HashMap<String, UserCharts> map = chartsUtil
                .getAllUserCharts(configuration.getMongoDatabase()
                        , configuration.getOlympicchartcollection());

        HashMap<String, String> idUserMap = new HashMap<>();

        for(Map.Entry<String, UserCharts>  entry : map.entrySet()) {
            String userName = entry.getKey();
            List<String> ids = entry.getValue().getIdList();
            for(String id : ids) {
                idUserMap.put(id, userName);
            }
        }

        return idUserMap;

    }

    public  HashMap<String, String>  getColorMap(Object colorMap) {

        HashMap<String, String> userColorMap = new HashMap<>();

        String colorData = colorMap.toString();
        JSONArray jsonArr = new JSONArray(colorData);


        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject object = jsonArr.getJSONObject(i);
            String userName = object.get("userName").toString();
            String color = object.get("color").toString();
            userColorMap.put(userName, color);
        }

        return userColorMap;
    }

    public HashMap<Integer, List<IdCoordinates>> getCoordinates(Double descWeight, Double attrWeight, Double chartEncodingWeight, Object colorMap) {

        HashMap<String, String> userColorMap = getColorMap(colorMap);
        HashMap<String, String> idUserMap = getIdUserMap();

        List<IdCoordinates> coordinatesList = coordinatesUtil.calculateCoordinates(descWeight, attrWeight, chartEncodingWeight);

        List<IdCoordinates> clusteredCoordinates = clustering.getClusteredCoordinates(4, 20, coordinatesList);

//        coordinatesScalingUtil.setCoordinatesList(clusteredCoordinates);
//
//        clusteredCoordinates = coordinatesScalingUtil.getScaledCoordinates(configuration.getMdsScalingConfig());


        HashMap<Integer, List<IdCoordinates>> coordinatesHashMap = new HashMap<>();

        for(IdCoordinates idCoordinate : clusteredCoordinates) {

            idCoordinate.setColor(userColorMap.get(idUserMap.get(idCoordinate.getId())));
            idCoordinate.setUserName(idUserMap.get(idCoordinate.getId()));

            if(coordinatesHashMap.containsKey(idCoordinate.getClusterId())) {

                List<IdCoordinates> coordinates = coordinatesHashMap.get(idCoordinate.getClusterId());
                coordinates.add(idCoordinate);
                coordinatesHashMap.put(idCoordinate.getClusterId(), coordinates);

            } else {

                List<IdCoordinates> coordinates = new ArrayList<>();
                coordinates.add(idCoordinate);
                coordinatesHashMap.put(idCoordinate.getClusterId(), coordinates);

            }
        }

        int clusterSize = coordinatesHashMap.keySet().size();
        ScalingConfig scalingConfig = configuration.getMdsScalingConfig();
        ScalingConfig newScalingConfig = configuration.getMdsScalingConfig();
        Double startX = scalingConfig.getXmin();
        Double stepX = (scalingConfig.getXmax() - scalingConfig.getXmin()) / clusterSize;
        Double startY = scalingConfig.getYmin();
        Double stepY = (scalingConfig.getYmax() - scalingConfig.getYmin()) / clusterSize;
        for(Map.Entry<Integer, List<IdCoordinates>> entry : coordinatesHashMap.entrySet()) {

            coordinatesScalingUtil.setCoordinatesList(entry.getValue());
            Double xMin = startX + 5;
            Double xMax = startX + stepX - 5;
            Double yMin = startY + 5;
            Double yMax = startY + stepY - 5;

            startX += stepX;
            startY += stepY;

            List<IdCoordinates> newIdCoordinatesList = coordinatesScalingUtil.getScaledCoordinates(xMin, xMax, yMin, yMax);

            coordinatesHashMap.put(entry.getKey(), newIdCoordinatesList);
        }
        return coordinatesHashMap;
    }
}
