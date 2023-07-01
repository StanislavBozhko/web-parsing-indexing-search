package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteDB;

import javax.transaction.Transactional;
import java.util.List;

/**
 * Интерфейс для работы с БД, содержит базовые метод save(T), Optional<T> findById(ID id) и прочие
 * Возможно добавлять свои собственные запросы в формате HQL или SQL
 */

@Repository
public interface SiteRepository extends JpaRepository<SiteDB, Integer> {

        /**
         * @param namePart часть слова
         * @param limit макс количество результатов
         * @return список подходящих слов
         *
         * <p>Для создания SQL запроса, необходимо указать nativeQuery = true</p>
         * <p>каждый параметр в SQL запросе можно вставить, используя запись :ИМЯ_ПЕРЕМEННОЙ
         * перед именем двоеточие, так hibernate поймет, что надо заменить на значение переменной</p>
         */
        @Query(value = "SELECT * from site where name LIKE %:namePart% LIMIT :limit", nativeQuery = true)
        List<SiteDB> findAllContains(String namePart, int limit);

        SiteDB findByUrl(String url);
        SiteDB findById(int id);

        @Transactional
        void deleteByUrl(String url);
}
