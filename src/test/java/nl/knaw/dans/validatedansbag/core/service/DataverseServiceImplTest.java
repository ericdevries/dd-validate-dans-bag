package nl.knaw.dans.validatedansbag.core.service;

import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import nl.knaw.dans.lib.dataverse.DataverseException;
import nl.knaw.dans.lib.dataverse.SearchOptions;
import nl.knaw.dans.lib.dataverse.model.search.DatasetResultItem;
import nl.knaw.dans.lib.dataverse.model.search.SearchItemType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

class DataverseServiceImplTest {

    DataverseClient getClient() {
        var config = new DataverseClientConfig(URI.create("http://localhost:9097"));
        return new DataverseClient(config);
    }

//    @Test
    void testSearch() throws IOException, DataverseException {
        var client = getClient();
        var options = new SearchOptions();
        options.setTypes(List.of(SearchItemType.dataset));
        var response = client.search().find("text");

        var items = response.getData().getItems().stream()
            .filter(f -> f instanceof DatasetResultItem)
            .map(f -> (DatasetResultItem) f)
            .collect(Collectors.toList());

        for (var item : items) {
            System.out.println("ITEM: " + item);
            System.out.println(item.getGlobalId());
        }
    }
}