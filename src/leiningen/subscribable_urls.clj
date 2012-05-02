(ns leiningen.subscribable-urls
  (:require
   [cemerick.pomegranate.aether :as aether]
   [clojure.string :as string]
   [clojure.walk]
   [clojure.xml :as xml]
   [leiningen.core.classpath :as classpath])
  (:import
   (java.io File)
   (java.util.jar JarFile)))

(defn get-entry [^JarFile jar, ^String name]
  (-> jar (.getEntry name)))

(def tag-content (juxt :tag (comp first :content)))

(defn pom->coordinates [pom-xml]
  (let [coords (->> pom-xml
                    :content
                    (filter (fn [{:keys [tag]}]
                              (#{:groupId :artifactId :version} tag)))
                    (map tag-content)
                    (into {}))]
    {:group    (:groupId coords)
     :artifact (:artifactId coords)
     :version  (:version coords)}))

(defn safe-name [x]
  (if (string? x)
    x
    (name x)))

(defn depvec->coordinates [[dep version]]
  {:group    (or (namespace dep)
                 (name dep))
   :artifact (safe-name dep)
   :version  version})

(defn fetch-pom* [{:keys [group artifact version repositories]}]
  (try
    (let [dep (symbol (safe-name group)
                      (safe-name artifact))
          d (aether/resolve-dependencies :coordinates [[dep version :extension "pom"]]
                                         :offline? true
                                         :repositories (->> repositories
                                                            (clojure.walk/postwalk (fn [x]
                                                                                     (if-not (and (keyword? x)
                                                                                                  (-> x
                                                                                                      str
                                                                                                      (string/starts-with? ":env/")))
                                                                                       x
                                                                                       (-> x
                                                                                           name
                                                                                           string/upper-case System/getenv))))))
          [file] (->> d
                      (aether/dependency-files)
                      ;; possible we could get the wrong one here?
                      (filter (fn [x]
                                (-> x str (string/ends-with? ".pom")))))]
      (xml/parse file))
    (catch Exception e
      ;; these two exceptions are irrelevant cases, given resolution is offline.
      ;; they come when the resolution algo determines different resolved artifacts.
      (when-not (#{org.eclipse.aether.resolution.DependencyResolutionException
                   org.eclipse.aether.resolution.ArtifactResolutionException} (class e))
        (binding [*out* *err*]
          (println "#   "
                   (str group)
                   (str artifact)
                   (class e)
                   (-> e .getMessage)
                   (-> e .printStackTrace)))))))

(def fetch-pom (memoize fetch-pom*))

(defn get-parent [pom]
  (when-let [parent-tag (->> pom
                             :content
                             (filter (comp #{:tag} :parent))
                             first)]
    (when-let [parent-coords (->> parent-tag
                                  pom->coordinates)]
      (fetch-pom parent-coords))))

(defn pom-url-elems [{:keys [content]}]
  (let [bad-groupId (->> content
                         (filter (fn [{:keys [tag content]}]
                                   (and (= tag :groupId)
                                        (= content ["org.sonatype.oss"]))))
                         (first)
                         (some?))
        bad-artifactId (->> content
                            (filter (fn [{:keys [tag content]}]
                                      (and (= tag :artifactId)
                                           (= content ["oss-parent"]))))
                            (first)
                            (some?))]
    (if (and bad-groupId bad-artifactId)
      []
      (for [elem content
            :when (= (:tag elem) :url)
            url (:content elem)]
        url))))

(defn pom->urls [pom]
  (->> pom pom-url-elems (string/join "") vector))

(defn get-pom [dep, ^JarFile file]
  (let [{:keys [group artifact]} (depvec->coordinates dep)
        pom-path (format "META-INF/maven/%s/%s/pom.xml" group artifact)
        pom (get-entry file pom-path)]
    (and pom (-> file
                 (.getInputStream pom)
                 xml/parse))))

(defn try-pom [[dep file] {:keys [recursive?]
                           :as   opts}]
  (let [packaged-poms (cond->> (get-pom dep file)
                        recursive?       (iterate get-parent)
                        recursive?       (take-while identity)
                        (not recursive?) vector)
        source-poms (cond->> (fetch-pom (merge opts (depvec->coordinates dep)))
                      recursive?       (iterate get-parent)
                      recursive?       (take-while identity)
                      (not recursive?) vector)]
    (some->> (concat packaged-poms source-poms)
             (map pom->urls)
             (apply concat)
             (some identity)
             (string/trim))))

(defn formatter [format lines]
  (->> lines
       (keep (fn [[artifact version url]]
               url))
       (filter (fn [s]
                 (or (string/starts-with? s "http://github.com/")
                     (string/starts-with? s "https://github.com/"))))
       (map (fn [s]
              (if (string/starts-with? s "http://")
                (string/replace-first s #"^http://" "https://")
                s)))
       (distinct)
       (sort)
       (map (fn [s]
              (let [ensured (if (string/ends-with? s "/")
                              s
                              (str s "/"))
                    atomized (str ensured "releases.atom")]
                (case format
                  "feed" (str "/feed " atomized)
                  "curl" (str
                          "curl -X POST \"https://slack.com/api/chat.command?token=$TOKEN&channel=$CHANNEL_ID&command=/feed&text="
                          atomized
                          "&pretty=1\"")))))))

(defn subscribable-urls
  "USAGE: lein subscribable-urls :format {feed,curl} :recursive {true,false}"
  [project & {format ":format"
              recursive? ":recursive"}]
  (assert (#{"feed" "curl"} format)
          (#{"true" "false"} recursive?))

  (let [recursive? (read-string recursive?)
        roots (->> project
                   :dependencies
                   (map (fn [[a b]]
                          [(name a), b]))
                   set)
        deps (cond->> (#'classpath/get-dependencies :dependencies :managed-dependencies project)
               (not recursive?) (filter (fn [[[a b] v]]
                                          (let [v [(name a) b]]
                                            (roots v))))
               (not recursive?) (into {}))
        deps (->> deps
                  aether/dependency-files
                  (map (fn [^File f]
                         (JarFile. f)))
                  (zipmap (keys deps)))]
    (->> deps
         (map #(conj % (try-pom % {:repositories (:repositories project)
                                   :recursive?   recursive?})))
         (map (fn [[[artifact version] file url]]
                [artifact version url]))
         (formatter format)
         (run! println))))
