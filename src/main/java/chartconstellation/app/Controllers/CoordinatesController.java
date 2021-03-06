package chartconstellation.app.Controllers;

import chartconstellation.app.appconfiguration.Configuration;
import chartconstellation.app.entities.*;
import chartconstellation.app.entities.response.Cluster;
import chartconstellation.app.entities.response.IdCoordinates;
import chartconstellation.app.entities.response.OutputResponse;
import chartconstellation.app.util.ChartsUtil;
import chartconstellation.app.util.ClusterUtil;
import chartconstellation.app.util.CoordinatesUtil;
import chartconstellation.app.util.DbUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping("/coordinates")
public class CoordinatesController {

    @Autowired
    Configuration configuration;

    @Autowired
    DbUtil dbUtil;

    @Autowired
    CoordinatesUtil coordinatesUtil;

    @Autowired
    ClusterUtil clusterUtil;

    @RequestMapping(value="/getCoordinates", method= RequestMethod.GET)
    @ResponseBody
    public OutputResponse coordinates(@RequestParam("descWeight") Double descWeight,
                                                       @RequestParam("attrWeight") Double attrWeight,
                                                       @RequestParam("chartEncodingWeight") Double chartEncodingWeight,
                                                       @RequestParam("colorMap") Object colorMap) {

        List<FeatureVector> featurevectors = dbUtil.getFeaturesFromCollection(configuration.getMongoDatabase(),
                configuration.getTotalFeatureCollection());

        HashMap<Integer, List<IdCoordinates>> coordinatesMap = coordinatesUtil.getCoordinates(3, featurevectors, descWeight, attrWeight, chartEncodingWeight, colorMap);

        List<Chart> chartObjs = dbUtil.getAttributesFromCollection(configuration.getMongoDatabase(), configuration.getOlympicchartcollection());

        List<Cluster> clusterList = clusterUtil.generateClusterInfo(coordinatesMap, chartObjs);

        OutputResponse outputResponse = new OutputResponse();
        outputResponse.setCoordinatesList(coordinatesMap.values());
        outputResponse.setClusters(clusterList);

        System.out.println(outputResponse.toString());

        return outputResponse;
    }

    @RequestMapping(value="/updateFilter", method= RequestMethod.GET)
    public OutputResponse filterCoordinates(@RequestParam("filter") String filterMap, @RequestParam("colorMap") Object colorMap) {
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
            List<FeatureVector> featurevectors = dbUtil.getFeaturesFromCollection(configuration.getMongoDatabase(),
                    configuration.getTotalFeatureCollection());

            Set<String> filteredIds = new HashSet<>();

            for(Chart chartObj : chartObjs) {
                filteredIds.add(chartObj.getId());
            }

            List<FeatureVector> filteredFeatureVectors = new ArrayList<>();
            for(FeatureVector featureVector : featurevectors) {
                if(filteredIds.contains(featureVector.getId())) {
                    List<IdFeatures> idfeatures = featureVector.getFeatures();
                    List<IdFeatures> filteredidfeatures = new ArrayList<>();
                    for(IdFeatures idFeature : idfeatures) {
                        if(filteredIds.contains(idFeature.getId())) {
                            filteredidfeatures.add(idFeature);
                        }
                    }
                    featureVector.setFeatures(filteredidfeatures);
                    filteredFeatureVectors.add(featureVector);
                }
            }

//            System.out.println("Filterd feature vectors "+filteredFeatureVectors.size());
//            System.out.println(filteredFeatureVectors);

            HashMap<Integer, List<IdCoordinates>> coordinatesMap = coordinatesUtil.getCoordinates(2, filteredFeatureVectors, 0.4, 0.4,0.2, colorMap);
//            System.out.println("Filtered coordinates map "+coordinatesMap);
//            System.out.println(coordinatesMap);
            List<Cluster> clusterList = clusterUtil.generateClusterInfo(coordinatesMap, chartObjs);

            //System.out.println(clusterList);
            //System.out.println(coordinatesMap.values());

            OutputResponse outputResponse = new OutputResponse();
            outputResponse.setCoordinatesList(coordinatesMap.values());
            outputResponse.setClusters(clusterList);

            System.out.println(outputResponse.toString());
            return outputResponse;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
    }
}
