package somun.appengine.api.v1;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import somun.appengine.AppengineUtils;
import somun.common.util.RandomUti;

@Slf4j
@Controller
@CrossOrigin(origins = "*")
@RequestMapping(path="Engine/V1/")
@Api(value = "Engine/V1/", description = "Search Indexing Service", tags = {"SearchIndex"})
@ApiResponses(value = {
    @ApiResponse(code = 400, message = "Wrong Type Parameter"),
    @ApiResponse(code = 404, message = "Does not exists User"),
    @ApiResponse(code = 500, message = "Server Error")})
public class IndexRestService {

    @Transactional
    @PostMapping("createContentIndex")
    @ResponseBody
    @ApiOperation(value="", notes = "index 생성")
    public String createContentIndex(){

        String indexName = "contentIndex_1";
        List<Integer> integerList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        integerList.stream().map((d)->{

            Document document = Document.newBuilder()
                                     .setId(String.valueOf(RandomUti.randomNumber(10)))
                                     .addField(Field.newBuilder().setName("desc").setText("desc1_" + RandomUti.randomString(10)))
                                     .addField(Field.newBuilder().setName("desc2").setText("desc2_" + RandomUti.randomString(10)))
                                     .build();
            try {
                AppengineUtils.indexADocument(indexName, document);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return d;
        });
        Document document =
            Document.newBuilder()
                    .setId("id_1")
                    .addField(Field.newBuilder().setName("desc").setText("�󼼼��� �󼼼���"))
                    .build();
        try {
            AppengineUtils.indexADocument(indexName, document);
        } catch (InterruptedException e) {

            return "Interrupted";
        }
        log.info("##### Indexed a new document.");
        // [START get_document]
        IndexSpec indexSpec = IndexSpec.newBuilder().setName(indexName).build();
        Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

        // Fetch a single document by its  doc_id
        Document doc = index.get("id_1");

        // Fetch a range of documents by their doc_ids
        GetResponse<Document> docs =
            index.getRange(GetRequest.newBuilder().setStartId("id_1").setLimit(100).build());
        // [END get_document]
        return ("myField: " + docs.getResults().get(0).getOnlyField("desc").getText());

    }
}
