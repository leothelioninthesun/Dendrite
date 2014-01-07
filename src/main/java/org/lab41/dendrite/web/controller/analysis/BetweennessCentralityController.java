package org.lab41.dendrite.web.controller.analysis;

import org.lab41.dendrite.graph.DendriteGraph;
import org.lab41.dendrite.models.GraphMetadata;
import org.lab41.dendrite.models.JobMetadata;
import org.lab41.dendrite.models.ProjectMetadata;
import org.lab41.dendrite.services.MetadataService;
import org.lab41.dendrite.services.MetadataTx;
import org.lab41.dendrite.services.analysis.BetweennessCentralityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BetweennessCentralityController {

    @Autowired
    MetadataService metadataService;

    @Autowired
    BetweennessCentralityService betweennessCentralityService;

    @RequestMapping(value = "/api/graphs/{graphId}/analysis/jung-betweenness-centrality", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> jungBetweennessCentrality(@PathVariable String graphId) throws Exception {

        Map<String, Object> response = new HashMap<>();

        DendriteGraph graph = metadataService.getGraph(graphId);
        if (graph == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        MetadataTx tx = metadataService.newTransaction();

        GraphMetadata graphMetadata = tx.getGraph(graphId);
        if (graphMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing graph metadata '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        ProjectMetadata projectMetadata = graphMetadata.getProject();
        if (projectMetadata == null) {
            response.put("status", "error");
            response.put("msg", "missing project metadata for graph '" + graphId + "'");
            tx.rollback();
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        JobMetadata jobMetadata = tx.createJob(projectMetadata);

        response.put("status", "ok");
        response.put("msg", "job submitted");
        response.put("jobId", jobMetadata.getId());

        tx.commit();

        // We can't pass the values directly because they'll live in a separate thread.
        betweennessCentralityService.jungBetweennessCentrality(graph, jobMetadata.getId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
