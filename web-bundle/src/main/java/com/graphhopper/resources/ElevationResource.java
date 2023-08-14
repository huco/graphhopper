package com.graphhopper.resources;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.GHRequestTransformer;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;

@Path("elevation")
public class ElevationResource {

    private static final Logger logger = LoggerFactory.getLogger(RouteResource.class);

    private final GraphHopper graphHopper;
    private final GHRequestTransformer ghRequestTransformer;

    @Inject
    public ElevationResource(GraphHopper graphHopper, GHRequestTransformer ghRequestTransformer) {
        this.graphHopper = graphHopper;
        this.ghRequestTransformer = ghRequestTransformer;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doPost(@NotNull GHRequest request, @Context HttpServletRequest httpReq) {
        ElevationProvider elevationProvider = graphHopper.getElevationProvider();
        if (elevationProvider == null) {
            throw new IllegalArgumentException("Elevation not supported!");
        }

        StopWatch sw = new StopWatch().start();
        request = ghRequestTransformer.transformRequest(request);

        PointList newPoints = new PointList(request.getPoints().size(), true);

        for (GHPoint p : request.getPoints()) {
            double elevation = elevationProvider.getEle(p.lat, p.lon);
            newPoints.add(p.lat, p.lon, elevation);
        }

        double took = sw.stop().getMillisDouble();

        logger.info((httpReq.getRemoteAddr() + " " + httpReq.getLocale() + " " + httpReq.getHeader("User-Agent"))
                + " points[" + newPoints.size() + "], took: " + String.format("%.1f", took) + "ms");

        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.putPOJO("points", newPoints.toLineString(true));

        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
    }
}
