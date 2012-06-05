<div class="clearfix #{ifError 'newPassword'} error #{/ifError}">
    <label for="newPassword">&{'securesocial.newPassword'}</label>

    <div class="input">
        <input id="newPassword" class="large" name="newPassword" type="password"
               value="${flash.newPassword}"/>
    #{ifError 'newPassword'}
        <span class="help-inline">#{error 'newPassword'/}</span>
    #{/ifError}
    </div>
</div>

<div class="clearfix #{ifError 'confirmPassword'} error #{/ifError}">
    <label for="confirmPassword">&{'securesocial.confirmPassword'}</label>

    <div class="input">
        <input id="confirmPassword" class="large" name="confirmPassword" type="password"
               value="${flash.confirmPassword}"/>
    #{ifError 'confirmPassword'}
        <span class="help-inline">#{error 'confirmPassword'/}</span>
    #{/ifError}
    </div>
</div>
