package ca.corefacility.bioinformatics.irida.plugins;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.PostProcessingException;
import ca.corefacility.bioinformatics.irida.model.sample.MetadataTemplateField;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.MetadataEntry;
import ca.corefacility.bioinformatics.irida.model.sample.metadata.PipelineProvidedMetadataEntry;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisOutputFile;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.type.AnalysisType;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.pipeline.results.updater.AnalysisSampleUpdater;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;

/**
 * This implements a class used to perform post-processing on the analysis
 * pipeline results to extract information to write into the IRIDA metadata
 * tables. Please see
 * <https://github.com/phac-nml/irida/blob/development/src/main/java/ca/corefacility/bioinformatics/irida/pipeline/results/AnalysisSampleUpdater.java>
 * or the README.md file in this project for more details.
 */
public class SeroTypeFinderPluginUpdater implements AnalysisSampleUpdater {

    private final MetadataTemplateService metadataTemplateService;
    private final SampleService sampleService;
    private final IridaWorkflowsService iridaWorkflowsService;

    /**
     * Builds a new {@link MLSTPluginUpdater} with the given services.
     *
     * @param metadataTemplateService The metadata template service.
     * @param sampleService           The sample service.
     * @param iridaWorkflowsService   The irida workflows service.
     */
    public SeroTypeFinderPluginUpdater(MetadataTemplateService metadataTemplateService, SampleService sampleService,
                             IridaWorkflowsService iridaWorkflowsService) {
        this.metadataTemplateService = metadataTemplateService;
        this.sampleService = sampleService;
        this.iridaWorkflowsService = iridaWorkflowsService;
    }

    /**
     * Code to perform the actual update of the {@link Sample}s passed in the
     * collection.
     *
     * @param samples  A collection of {@link Sample}s that were passed to this
     *                 pipeline.
     * @param analysisSubmission The {@link AnalysisSubmission} object corresponding to this
     *                 analysis submission.
     */
    @Override
    public void update(Collection<Sample> samples, AnalysisSubmission analysisSubmission) throws PostProcessingException {
        

        final Sample sample = samples.iterator().next();

        // extracts paths to the analysis result files
        AnalysisOutputFile seroTypeFormattedFile = analysisSubmission.getAnalysis().getAnalysisOutputFile("formatted_serotypefinder_results.tsv");
        Path seroTypeFormattedPath = seroTypeFormattedFile.getFile();

        try {
            Map<String, MetadataEntry> metadataEntries = new HashMap<>();

            // get information about the workflow (e.g., version and name)
            IridaWorkflow iridaWorkflow = iridaWorkflowsService.getIridaWorkflow(analysisSubmission.getWorkflowId());
            String workflowVersion = iridaWorkflow.getWorkflowDescription().getVersion();
            String workflowName = iridaWorkflow.getWorkflowDescription().getName();

            // gets information from the "formatted_output.tsv" output file and constructs metadata
            // objects
            // Map<String, String> amrValues = parseAMRFile(amrFormattedPath);
            Map<String, String> seroTypeValues = parseSeroTypeFile(seroTypeFormattedPath);

            for (String seroTypeField : seroTypeValues.keySet()) {
                final String seroTypeValue = seroTypeValues.get(seroTypeField);

                PipelineProvidedMetadataEntry seroTypeEntry = new PipelineProvidedMetadataEntry(seroTypeValue, "text", analysisSubmission);

                // key will be string like 'mlst/0.1.0/scheme'
                //String key = workflowName.toLowerCase() + "/" + amrField;
                String key = "SeroTypeFinder/" + seroTypeField;
                metadataEntries.put(key, seroTypeEntry);
            }

            //convert the string/entry Map to a Set of MetadataEntry.  This has the same function as the old metadataTemplateService.getMetadataMap
            Set<MetadataEntry> metadataSet = metadataTemplateService.convertMetadataStringsToSet(metadataEntries);

            // merges with existing sample metadata and does an update of the sample metadata.  This has the function of the old sample.mergeMetadata and sampleService.updateFields
            sampleService.mergeSampleMetadata(sample,metadataSet);
    
        } catch (IOException e) {
            throw new PostProcessingException("Error parsing hash file", e);
        } catch (IridaWorkflowNotFoundException e) {
            throw new PostProcessingException("Could not find workflow for id=" + analysisSubmission.getWorkflowId(), e);
        }
    }


    // Parsing the output file and storing the results in hash-map
    private Map<String, String> parseSeroTypeFile(Path serotypeFile) throws IOException, PostProcessingException {
       // Map<String, String> amrResults = new HashMap<>();
        Map<String, String> seroTyperResults = new HashMap<>();

        //BufferedReader amrReader = new BufferedReader(new FileReader(amrFile.toFile()));
        BufferedReader serotypeReader = new BufferedReader(new FileReader(serotypeFile.toFile()));

        try {

            String[] serotypeFields = {
                    "SeroType",
            };

            String word = serotypeReader.readLine();
            String data_line = serotypeReader.readLine();
            String[] serotypeValues = data_line.split("\t");


            // Start index at 1, skip sample_id from mlstFile
            for (int i = 0; i < serotypeFields.length; i++) { 
                seroTyperResults.put(serotypeFields[i], serotypeValues[i]);
            }

        } finally {
            // make sure to close, even in cases where an exception is thrown
            serotypeReader.close();
        }

        return seroTyperResults;
    }

    @Override
    public AnalysisType getAnalysisType() {
        return SeroTypeFinderPlugin.SeroType_Finder;
    }
}