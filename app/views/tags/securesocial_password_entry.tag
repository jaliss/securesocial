<div class="control-group #{ifError 'newPassword'} error #{/ifError}">
    <label class="control-label" for="newPassword">&{'securesocial.newPassword'}</label>

    <div class="controls">
        <input id="newPassword" class="input-large" name="newPassword" type="password"
               value="${flash.newPassword}"/>
    #{ifError 'newPassword'}
        <span class="help-inline">#{error 'newPassword'/}</span>
    #{/ifError}
    </div>
</div>

<div class="control-group #{ifError 'confirmPassword'} error #{/ifError}">
    <label class="control-label" for="confirmPassword">&{'securesocial.confirmPassword'}</label>

    <div class="controls">
        <input id="confirmPassword" class="input-large" name="confirmPassword" type="password"
               value="${flash.confirmPassword}"/>
    #{ifError 'confirmPassword'}
        <span class="help-inline">#{error 'confirmPassword'/}</span>
    #{/ifError}
    </div>
</div>
