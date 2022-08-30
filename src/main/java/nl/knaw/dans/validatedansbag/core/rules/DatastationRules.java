package nl.knaw.dans.validatedansbag.core.rules;

public interface DatastationRules {

    BagValidatorRule organizationalIdentifierIsValid();

    BagValidatorRule isVersionOfIsAValidSwordToken();

    BagValidatorRule dataStationUserAccountIsAuthorized();

}
