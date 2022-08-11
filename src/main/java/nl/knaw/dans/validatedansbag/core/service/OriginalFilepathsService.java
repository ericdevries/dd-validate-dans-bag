package nl.knaw.dans.validatedansbag.core.service;

import java.nio.file.Path;
import java.util.List;

public interface OriginalFilepathsService {

    boolean exists(Path bagDir);

    List<OriginalFilePathItem> getMapping(Path bagDir);

    class OriginalFilePathItem {
        private Path originalFilename;
        private Path renamedFilename;
        public OriginalFilePathItem(Path originalFilename, Path renamedFilename) {
            this.originalFilename = originalFilename;
            this.renamedFilename = renamedFilename;
        }

        public Path getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(Path originalFilename) {
            this.originalFilename = originalFilename;
        }

        public Path getRenamedFilename() {
            return renamedFilename;
        }

        public void setRenamedFilename(Path renamedFilename) {
            this.renamedFilename = renamedFilename;
        }
    }
}
