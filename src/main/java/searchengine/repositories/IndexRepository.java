package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexDB;
import searchengine.model.LemmaDB;
import searchengine.model.PageDB;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface IndexRepository extends JpaRepository<IndexDB, Integer> {


    default List<IndexDB> findByLemmasAndPages(List<LemmaDB> lemmaList, List<PageDB> pageList) {
        Set<Integer> lemmaIds = lemmaList.stream().map(LemmaDB::getId).collect(Collectors.toSet());
        Set<Integer> pageIds = pageList.stream().map(PageDB::getId).collect(Collectors.toSet());

        return findAllByLemmaIdInAndPageIdIn(lemmaIds, pageIds);
    }

    List<IndexDB> findAllByLemmaIdInAndPageIdIn(Set<Integer> lemmaIds, Set<Integer> pageIds);

}
