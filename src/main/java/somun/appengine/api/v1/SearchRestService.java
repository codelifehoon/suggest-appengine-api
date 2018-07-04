package somun.appengine.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import somun.appengine.service.ContentSearchService;
import somun.appengine.service.repository.SearchIndexComb;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@Controller
@CrossOrigin(origins = "*")
@RequestMapping(path="Engine/V1/SE")
@Api(value = "Engine/V1/SE", description = "Search Service", tags = {"Search"})
@ApiResponses(value = {
    @ApiResponse(code = 400, message = "Wrong Type Parameter"),
    @ApiResponse(code = 404, message = "Does not exists User"),
    @ApiResponse(code = 500, message = "Server Error")})
public class SearchRestService {

    @Autowired
    ContentSearchService contentSearchService;

    @PostMapping("searchTotalSearchIndex")
    @ResponseBody
    @ApiOperation(value="", notes = "index 생성")
    @ApiImplicitParams({  @ApiImplicitParam(name = "page", dataType = "integer", paramType = "query", value = "Results page you want to retrieve (0..N)"),
                           @ApiImplicitParam(name = "size", dataType = "integer", paramType = "query", value = "Number of records per page.")
                       })
    public Page<SearchIndexComb> searchTotalSearchIndex(@RequestBody SearchIndexComb searchIndexComb
        , @ApiIgnore @PageableDefault(page=0, size=20) Pageable pageable) throws InterruptedException {

        return contentSearchService.searchTotalSearchIndex(searchIndexComb,new PageRequest(pageable.getPageNumber(), pageable.getPageSize()));

    }

}
