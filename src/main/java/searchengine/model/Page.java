package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})
@NoArgsConstructor(force = true)
@Setter
@Getter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private int id;
    @NotNull
    @Column(name = "site_id")
    private int siteId;
    @NotNull
    private String path;
    @NotNull
    private int code;
    @NotNull
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false, insertable = false, updatable = false)
    private SitePage sitePage;

    public Page(Page page) {
        this.id = page.getId();
        this.siteId = page.getSiteId();
        this.path = page.getPath();
        this.code = page.getCode();
        this.content = page.getContent();
        this.sitePage = page.getSitePage();
    }
}