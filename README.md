<h1 style="text-align: left;">Searchengine</h1>
<p>Данный проект реализует поисковый движок, предоставляющий пользователю специальный API со следующими основными функциями:</p>
<ul>
<li>предварительное индексирование сайтов;</li>
<li>выдача основных сведений по сайтам;</li>
<li>поиск ключевых слов в проиндексированных сайтах и предоставление их пользователю.</li>
</ul>
<p>&nbsp;</p>
<h2>Стэк используемых технологий</h2>
<p>Spring Framework, JPA, JSOUP, SQL, Morphology Library Lucene</p>
<p>&nbsp;</p>
<h2>Веб-интерфейс</h2>
<p>В проект входит веб-интерфейс, который позволяет управлять процессами, реализованными в движке.</p>
<p>Веб-интерфейс (frontend-составляющая) проекта представляет собой&nbsp;одну веб-страницу с тремя вкладками:</p>
<h3>Вкладка DASHBOARD</h3>
<img src="./images/dashboard.JPG" width="100%">
<p>Эта вкладка открывается по умолчанию. На ней&nbsp;отображается общая статистика по всем сайтам, а также детальная<br />статистика и статус по каждому из сайтов (статистика, получаемая по&nbsp;запросу <strong>/api/statistics</strong>).</p>
<h3>Вкладка MANAGEMENT</h3>
<img src="./images/management.JPG" width="100%">
<p>На этой вкладке находятся инструменты управления поисковым движком &mdash; запуск кнопка "<strong>Start Indexing</strong>" (запрос <strong><em>/startIndexing</em></strong>) и остановка кнопка "<strong>Stop Indexing</strong>"&nbsp;(запрос <strong><em>/stopIndexing</em></strong>) полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу указанную в поле "<strong>Add/update page:</strong>"&nbsp;кнопка "<strong>ADD/UPDATE</strong>"&nbsp; (запрос <strong><em>/indexPage/{pagePath}</em></strong>).</p>
<h3>Вкладка SEARCH</h3>
<img src="./images/search.JPG" width="100%">
<p>Эта вкладка предназначена для тестирования поискового движка. На ней находится поле поиска и выпадающий список с выбором сайта, по которому искать, а при нажатии на кнопку "<em><strong>SEARCH</strong>"</em> выводятся результаты поиска (по запросу <strong>/search</strong>).</p>
<p>&nbsp;</p>
<h2>Файл настройки</h2>
<p><strong><em>&nbsp;application.yaml</em></strong></p>
<h3>Раздел server</h3>
<p>В этом разделе задаётся параметр <em>port</em> &mdash; порт, через который контроллеры&nbsp;приложения "слушают" веб-запросы. Задавая разные порты, можно, например,&nbsp;из разных папок, в которых находятся файлы настройки, запустить несколько&nbsp;экземпляров приложения.</p>
<h3>Раздел spring</h3>
<p>Здесь задаются параметры для подключения к СУБД (логин, пароль, строка подключения и параметры Hibernate для работы с СУБД).</p>
<p>Внимание! Базу данных необходимо создать используя&nbsp;кодировку&nbsp;<strong>utf8mb4</strong></p>
<img src="./images/createSchema.JPG" width="100%">
<h3>Раздел connect-settings</h3>
<p>Здесь задаются параметры <strong>userAgent</strong> и <strong>referer</strong>&nbsp;соединения для получения контента веб-страницы</p>
<h3>Раздел search-settings</h3>
<p>Здесь&nbsp;задаются параметры, которые используются при&nbsp;поиске информации&nbsp;(по API-запросу<strong> /api/search</strong>):</p>
<ul>
<li>
<strong>sortByAbsoluteRelevance: true</strong> (default) 
<pre>если true - то выводит список найденных страниц отсортированных по Абсолютной релевантности<br />если false - то по Относительной релевантности</pre>
</li>
<li>
<strong>onlyFirstNormalWord: true</strong>  (default) 
<pre>если true - то берет только первую нормальную форму слова в качестве леммы для поиска<br />если false - то берет все нормальные формы слова в качестве леммы для поиска</pre>
</li>
<li>
<strong>percentFilteredLemmas: 75</strong>  (default) 
<pre>процент для исключения из полученного списка леммы, которые встречаются на
слишком большом количестве страниц&nbsp;</pre>
</li>
</ul>
<h3>Раздел indexing-settings</h3>
<h4>подраздел sites</h4>
<p>Список сайтов для индексации.</p>
<p>&nbsp;</p>
<p>&nbsp;</p>