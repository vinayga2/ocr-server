package dynamic.groovy

import com.optum.ocr.service.SecureService

class GSecureService extends SecureService {
    @Override
    String register(String msId) throws IllegalAccessException, IOException, InstantiationException {
        return "";
    }

    @Override
    String addLoginHistory(String msId) throws IllegalAccessException, IOException, InstantiationException {
        return "";
    }

    @Override
    byte[] downloadInactiveAccount() throws IllegalAccessException, IOException, InstantiationException {
        return null;
    }
}
