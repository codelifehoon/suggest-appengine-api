package somun.appengine.api.v1;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import somun.appengine.common.util.RandomUti;
import somun.appengine.config.properties.SomunProperties;
import somun.appengine.service.ContentSearchService;

@Slf4j
@Controller
@CrossOrigin(origins = "*")
@RequestMapping(path="Engine/V1/INDEX")
@Api(value = "Engine/V1/INDEX", description = "Document Index Service", tags = {"Document Index"})
@ApiResponses(value = {
    @ApiResponse(code = 400, message = "Wrong Type Parameter"),
    @ApiResponse(code = 404, message = "Does not exists User"),
    @ApiResponse(code = 500, message = "Server Error")})
public class IndexRestService {

    @Autowired
    SomunProperties somunProperties;
    @Autowired
    ContentSearchService contentSearchService;

    @PostMapping("mergeTotalSearchIndex")
    @ResponseBody
    @ApiOperation(value="", notes = "index 생성")
    public int mergeTotalSearchIndex() throws InterruptedException {

        return contentSearchService.mergeTotalSearchIndex(somunProperties.getApiServer() + "/Content/V1/indexDocList/2018-01-01/2019-01-01?page=0&size=100");

    }

//    @PostMapping("createContentIndex")
//    @ResponseBody
//    @ApiOperation(value="", notes = "index 생성")
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
