package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "page")
public class PageDB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = SiteDB.class)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteDB site;

    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci", nullable = false) //
    private String content;

    @Column(columnDefinition = "TEXT NOT NULL, UNIQUE KEY pathIndex (path(256), site_id)") //, Index(path(512))
    private String path;

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexDB> indexDBList = new ArrayList<>();

    public PageDB(SiteDB site, int code, String content, String path) {
        this.site = site;
        this.code = code;
        this.content = content;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageDB pageDB = (PageDB) o;
        return id == pageDB.id &&
                code == pageDB.code &&
                site.equals(pageDB.site) &&
                content.equals(pageDB.content) &&
                path.equals(pageDB.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, code, content, path);
    }

    @Override
    public String toString() {
        return "PageDB{" +
                "id=" + id +
                ", site=" + site +
                ", code=" + code +
                ", content='" + content + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
