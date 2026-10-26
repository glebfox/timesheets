/*
 * Copyright 2023 Gleb Gorelov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glebfox.timesheets.view.user;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.AccessManager;
import io.jmix.core.Metadata;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.action.ChangePasswordAction;
import io.jmix.securityflowui.action.ResetPasswordAction;
import io.jmix.securityflowui.action.ShowRoleAssignmentsAction;
import io.jmix.securityflowui.action.ShowUserSubstitutionsAction;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "users", layout = MainView.class)
@ViewController("ts_User.list")
@ViewDescriptor("user-list-view.xml")
@LookupComponent("usersDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class UserListView extends StandardListView<User> {

    @ViewComponent("usersDataGrid.showRoleAssignments")
    private ShowRoleAssignmentsAction<User> showRoleAssignmentsAction;
    @ViewComponent("usersDataGrid.changePassword")
    private ChangePasswordAction<User> changePasswordAction;
    @ViewComponent("usersDataGrid.resetPassword")
    private ResetPasswordAction<User> resetPasswordAction;
    @ViewComponent("usersDataGrid.showUserSubstitutionsAction")
    private ShowUserSubstitutionsAction<User> showUserSubstitutionsAction;
    @ViewComponent
    private DropdownButton additionalBtn;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private Metadata metadata;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        // Not at init: the dropdown gets its items only after the init event
        hideForbiddenSecurityActions();
    }

    @Supply(to = "usersDataGrid.username", subject = "renderer")
    private Renderer<User> usersDataGridUsernameRenderer() {
        return componentSupport.createUserRenderer();
    }

    private void hideForbiddenSecurityActions() {
        // The actions do not check whether the current user may open their views, so an action whose view is denied
        // would stay available and end on an access-denied page
        showRoleAssignmentsAction.setVisible(isViewPermitted("roleAssignmentView"));
        showUserSubstitutionsAction.setVisible(isViewPermitted("sec_UserSubstitution.view"));

        // Everyone may open the change password view, for their own password, so the password actions also need
        // permission to edit users
        boolean userEditPermitted = isUserEditPermitted();
        changePasswordAction.setVisible(userEditPermitted && isViewPermitted("changePasswordView"));
        resetPasswordAction.setVisible(userEditPermitted && isViewPermitted("resetPasswordView"));

        // Not left as an empty menu when all its items are hidden
        additionalBtn.setVisible(additionalBtn.getItems().stream().anyMatch(DropdownButtonItem::isVisible));
    }

    /**
     * @param viewId the id of a view, as the view policies of roles name it
     */
    private boolean isViewPermitted(String viewId) {
        UiShowViewContext viewContext = new UiShowViewContext(viewId);
        accessManager.applyRegisteredConstraints(viewContext);
        return viewContext.isPermitted();
    }

    private boolean isUserEditPermitted() {
        UiEntityContext entityContext = new UiEntityContext(metadata.getClass(User.class));
        accessManager.applyRegisteredConstraints(entityContext);
        return entityContext.isEditPermitted();
    }
}
